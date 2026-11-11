/**
 * ERP 金额/数量计算与格式化工具
 *
 * 口径严格对齐后端（后端单据成本口径）：
 * - 取整统一为远离 0 的四舍五入，等价于 BigDecimal.setScale(n, RoundingMode.HALF_UP)
 * - 行金额 amount = qty × price，两位小数 HALF_UP
 * - 每件运杂费 freightPerUnit = 分摊额 ÷ 数量，四位小数 HALF_UP
 * - 成本单价 costPrice = 不含税价（6 位中间量）+ 每件运杂费，四位小数 HALF_UP
 * - 不含税价仅在 taxType = VAT_INVOICE 且 taxRate > 0 时换算
 *
 * 运杂费分摊由后端统一计算（POST /erp/purchase/orders/preview-allocation），
 * 前端不再保留第二份实现，避免尾差规则与 MANUAL 校验漂移。
 */

const MONEY_SCALE = 2;
const QTY_SCALE = 3;
const COST_SCALE = 4;
/** 不含税价中间量精度，对齐后端 price.divide(..., 6, HALF_UP) */
const NO_TAX_SCALE = 6;

/**
 * 远离 0 的四舍五入，等价于 BigDecimal.setScale(scale, RoundingMode.HALF_UP)
 *
 * Math.round 向 +∞ 取整，负数会得到错误结果（-1.005 → -1）；
 * 这里先取绝对值再取整并还原符号，保证正负对称。
 * 使用 Number.EPSILON * abs（相对量级）而非 Number.EPSILON（绝对量级），
 * 避免大数被固定 EPSILON 淹没导致 .xx5 边界失效。
 */
function halfUp(value: number, scale: number): number {
  if (!Number.isFinite(value)) return 0;
  const sign = value < 0 ? -1 : 1;
  const abs = Math.abs(value);
  const p = 10 ** scale;
  return (sign * Math.round((abs + Number.EPSILON * abs) * p)) / p;
}

/**
 * 金额按两位小数 HALF_UP 取整，避免浮点误差累积
 */
export function roundMoney(value: number): number {
  return halfUp(value, MONEY_SCALE);
}

/**
 * 数量按三位小数 HALF_UP 取整
 */
export function roundQty(value: number): number {
  return halfUp(value, QTY_SCALE);
}

/**
 * 成本相关数值按四位小数 HALF_UP 取整（与后端 costPrice / freightPerUnit 一致）
 */
export function roundCost(value: number): number {
  return halfUp(value, COST_SCALE);
}

/**
 * 商品数量精度（md_product.qty_scale，DOC/26 §10）
 *
 * 只接受 0~3；未知（未选商品/商品已删）按整数 0 兜底，选中小数商品后自动切换精度。
 * 后端是唯一校验口径，前端不做二次截断。
 *
 * @param scale 商品数量精度位数（0=整数）
 * @returns 输入框应使用的精度位数
 */
export function qtyPrecision(scale?: number | null): number {
  if (scale === null || scale === undefined) return 0;
  const n = Number(scale);
  return Number.isInteger(n) && n >= 0 && n <= QTY_SCALE ? n : 0;
}

/**
 * 数量输入步进：整数商品 1，1/2/3 位小数分别为 0.1 / 0.01 / 0.001
 *
 * @param scale 商品数量精度位数（0=整数）
 * @returns 输入框步进值
 */
export function qtyStep(scale?: number | null): number {
  const precision = qtyPrecision(scale);
  return precision === 0 ? 1 : 1 / 10 ** precision;
}

/**
 * 按商品数量精度归一化数量
 *
 * 切换商品（qtyScale 变化）时把已录数量四舍五入到新精度，避免小数商品录入的
 * 1.25 挂在整数商品行上：el-input-number 只改显示值不回写 model，最终提交仍是 1.25
 * 被后端精度校验拒绝。仅在已选中商品时调用；清空商品不归一化。
 *
 * @param value 原始数量
 * @param scale 商品数量精度位数（0=整数）
 * @returns 按新精度 HALF_UP 取整后的数量
 */
export function normalizeQty(value: number | string | undefined | null, scale?: number | null): number {
  const num = Number(value ?? 0);
  if (!Number.isFinite(num)) return 0;
  return halfUp(num, qtyPrecision(scale));
}

/**
 * 含税价转不含税价，仅增值税发票且税率大于 0 时换算
 *
 * 与后端 ErpPurchaseServiceImpl#lineCostPrice 同构，两级取整：
 * 1. 税率 ÷ 100 → 6 位 HALF_UP（对齐 taxRate.divide(100, 6, HALF_UP)）
 * 2. 含税价 ÷ (1 + 税率比) → 6 位 HALF_UP（对齐 price.divide(..., 6, HALF_UP)）
 */
export function toNoTaxPrice(price: number, taxType: string | undefined, taxRate: number): number {
  if (!price) return 0;
  if (taxType !== 'VAT_INVOICE' || !taxRate || taxRate <= 0) return price;
  const taxRatio = halfUp(taxRate / 100, NO_TAX_SCALE);
  return halfUp(price / (1 + taxRatio), NO_TAX_SCALE);
}

/**
 * 采购行成本单价 = 不含税采购价（6 位中间量）+ 每件运杂费，四位小数 HALF_UP
 */
export function calcPurchaseCostPrice(price: number, taxType: string | undefined, taxRate: number, freightPerUnit: number): number {
  return halfUp(toNoTaxPrice(price, taxType, taxRate) + (freightPerUnit || 0), COST_SCALE);
}

/**
 * 毛利与毛利率
 */
export function calcGrossProfit(amount: number, costAmount: number): number {
  return roundMoney((amount || 0) - (costAmount || 0));
}

export function calcGrossMargin(amount: number, costAmount: number): number {
  if (!amount || amount <= 0) return 0;
  return roundMoney((((amount || 0) - (costAmount || 0)) / amount) * 100);
}

/**
 * 核销明细合计校验：Σ核销额必须等于单据金额
 */
export function isAllocationBalanced(total: number, allocations: { amount: number }[]): boolean {
  const sum = allocations.reduce((acc, item) => acc + (item.amount || 0), 0);
  return roundMoney(sum) === roundMoney(total || 0);
}

/**
 * 核销明细合计
 */
export function sumAllocation(allocations: { amount: number }[]): number {
  return roundMoney(allocations.reduce((acc, item) => acc + (item.amount || 0), 0));
}

/**
 * 格式化金额，固定两位小数并加千分位
 */
export function formatMoney(value: number | string | undefined | null): string {
  const num = Number(value ?? 0);
  if (!Number.isFinite(num)) return '0.00';
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: MONEY_SCALE,
    maximumFractionDigits: MONEY_SCALE
  });
}

/**
 * 格式化数量，固定三位小数
 */
export function formatQty(value: number | string | undefined | null): string {
  const num = Number(value ?? 0);
  if (!Number.isFinite(num)) return '0.000';
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: QTY_SCALE,
    maximumFractionDigits: QTY_SCALE
  });
}

/**
 * 按商品数量精度格式化数量
 *
 * 与开单页数量输入框口径一致：整数商品不带小数位，N 位小数固定 N 位（100 / 100.00）。
 * 避免整数商品被展示成 100.000，也避免同一行内「只读数量」与「数量输入框」位数不一致。
 * 无商品精度信息（如跨商品的合计）时不适用，仍用 formatQty。
 *
 * @param value 数量
 * @param scale 商品数量精度位数（0=整数）
 * @returns 千分位字符串，小数位固定为该精度
 */
export function formatQtyByScale(value: number | string | undefined | null, scale?: number | null): string {
  const num = Number(value ?? 0);
  const digits = qtyPrecision(scale);
  if (!Number.isFinite(num)) return (0).toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits });
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
}

/**
 * 格式化数量（去尾零）：整数不带小数位，小数保留实际有效位（最多 3 位）
 *
 * 用于数量展示列（历史价格、明细合计等），整数商品显示 1 而非 1.000。
 *
 * @param value 数量
 * @returns 千分位字符串，如 1 / 1.5 / 1.25
 */
export function formatQtyTrim(value: number | string | undefined | null): string {
  const num = Number(value ?? 0);
  if (!Number.isFinite(num)) return '0';
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: QTY_SCALE
  });
}

/**
 * 格式化百分比
 */
export function formatPercent(value: number | string | undefined | null): string {
  const num = Number(value ?? 0);
  if (!Number.isFinite(num)) return '0.00%';
  return `${num.toFixed(2)}%`;
}
