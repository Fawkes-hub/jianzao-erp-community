import { describe, expect, it } from 'vitest';
import {
  calcGrossMargin,
  calcGrossProfit,
  calcPurchaseCostPrice,
  formatMoney,
  formatPercent,
  formatQty,
  formatQtyByScale,
  formatQtyTrim,
  isAllocationBalanced,
  normalizeQty,
  roundCost,
  roundMoney,
  roundQty,
  qtyPrecision,
  qtyStep,
  sumAllocation,
  toNoTaxPrice
} from '../calc';

/**
 * 口径对齐后端 后端单据成本口径：
 * - 取整远离 0 四舍五入，等价 BigDecimal.setScale(n, RoundingMode.HALF_UP)
 * - 每件运杂费四位小数，成本单价四位小数
 * - 不含税换算仅当 taxType=VAT_INVOICE 且税率>0，且先 6 位中间量再 4 位结果
 */
describe('roundMoney', () => {
  it('保留两位小数并四舍五入', () => {
    expect(roundMoney(1.005)).toBe(1.01);
    expect(roundMoney(2.675)).toBe(2.68);
    expect(roundMoney(10)).toBe(10);
  });

  it('负数远离 0 四舍五入（对齐 BigDecimal HALF_UP）', () => {
    expect(roundMoney(-1.005)).toBe(-1.01);
    expect(roundMoney(-0.005)).toBe(-0.01);
    expect(roundMoney(-2.675)).toBe(-2.68);
  });

  it('大数 .xx5 边界仍然正确', () => {
    expect(roundMoney(8192.005)).toBe(8192.01);
    expect(roundMoney(-8192.005)).toBe(-8192.01);
  });

  it('非有限数返回 0', () => {
    expect(roundMoney(Number.NaN)).toBe(0);
    expect(roundMoney(Number.POSITIVE_INFINITY)).toBe(0);
  });
});

describe('roundQty', () => {
  it('保留三位小数', () => {
    expect(roundQty(1.2345)).toBe(1.235);
    expect(roundQty(0.0005)).toBe(0.001);
  });

  it('负数远离 0 四舍五入', () => {
    expect(roundQty(-1.2345)).toBe(-1.235);
    expect(roundQty(-0.0005)).toBe(-0.001);
  });
});

describe('roundCost', () => {
  it('保留四位小数', () => {
    expect(roundCost(31.75904)).toBe(31.759);
    expect(roundCost(0.123456)).toBe(0.1235);
  });

  it('负数远离 0 四舍五入', () => {
    expect(roundCost(-31.75904)).toBe(-31.759);
    expect(roundCost(-0.123456)).toBe(-0.1235);
  });
});

describe('toNoTaxPrice', () => {
  it('增值税发票按税率换算不含税价', () => {
    expect(toNoTaxPrice(11.3, 'VAT_INVOICE', 13)).toBe(10);
  });

  it('不含税发票直接返回原价', () => {
    expect(toNoTaxPrice(11.3, 'NO_TAX', 13)).toBe(11.3);
  });

  it('普通发票直接返回原价', () => {
    expect(toNoTaxPrice(11.3, 'GENERAL_INVOICE', 13)).toBe(11.3);
  });

  it('税率为 0 时不换算', () => {
    expect(toNoTaxPrice(11.3, 'VAT_INVOICE', 0)).toBe(11.3);
  });

  it('中间量先取 6 位 HALF_UP，对齐后端 lineCostPrice', () => {
    // 后端：price.divide(1 + 13/100, 6, HALF_UP) = 3.008850
    expect(toNoTaxPrice(3.4, 'VAT_INVOICE', 13)).toBe(3.00885);
    expect(toNoTaxPrice(28, 'VAT_INVOICE', 13)).toBe(24.778761);
  });
});

describe('calcPurchaseCostPrice', () => {
  it('成本单价 = 不含税价 + 每件运杂费', () => {
    expect(calcPurchaseCostPrice(11.3, 'VAT_INVOICE', 13, 0)).toBe(10);
    expect(calcPurchaseCostPrice(11.3, 'VAT_INVOICE', 13, 0.6667)).toBe(10.6667);
  });

  it('不含税发票按原价加运杂费', () => {
    expect(calcPurchaseCostPrice(28, 'NO_TAX', 13, 1)).toBe(29);
  });

  it('两级取整与后端口径一致', () => {
    // 后端：base = 4.53 / (1 + 13/100) = 4.008850 → setScale(4) = 4.0089
    // 旧前端一次算到 4 位会得到 4.0088，差 1e-4
    expect(calcPurchaseCostPrice(4.53, 'VAT_INVOICE', 13, 0)).toBe(4.0089);
    // 后端：base = 3.40 / (1 + 13/100) = 3.008850 → setScale(4) = 3.0089
    expect(calcPurchaseCostPrice(3.4, 'VAT_INVOICE', 13, 0)).toBe(3.0089);
    // 后端：base = 3.008850 + 1.0000 → setScale(4) = 4.0089
    expect(calcPurchaseCostPrice(3.4, 'VAT_INVOICE', 13, 1.0)).toBe(4.0089);
    // 后端：base = 24.778761 + 1.0000 → setScale(4) = 25.7788
    expect(calcPurchaseCostPrice(28, 'VAT_INVOICE', 13, 1.0)).toBe(25.7788);
  });
});

describe('毛利计算', () => {
  it('毛利 = 金额 − 成本金额', () => {
    expect(calcGrossProfit(500, 158.8)).toBe(341.2);
  });

  it('毛利率按百分比返回', () => {
    expect(calcGrossMargin(500, 158.8)).toBe(68.24);
  });

  it('金额为 0 时毛利率返回 0', () => {
    expect(calcGrossMargin(0, 100)).toBe(0);
  });
});

describe('核销金额校验', () => {
  it('合计等于单据金额时平衡', () => {
    expect(isAllocationBalanced(1000, [{ amount: 600 }, { amount: 400 }])).toBe(true);
  });

  it('合计不等于单据金额时不平衡', () => {
    expect(isAllocationBalanced(1000, [{ amount: 600 }, { amount: 300 }])).toBe(false);
  });

  it('合计按两位小数比较，规避浮点误差', () => {
    expect(isAllocationBalanced(0.3, [{ amount: 0.1 }, { amount: 0.2 }])).toBe(true);
  });

  it('sumAllocation 返回两位小数合计', () => {
    expect(sumAllocation([{ amount: 0.1 }, { amount: 0.2 }])).toBe(0.3);
  });
});

describe('商品数量精度（DOC/26 §10）', () => {
  it('0~3 原样返回', () => {
    expect(qtyPrecision(0)).toBe(0);
    expect(qtyPrecision(1)).toBe(1);
    expect(qtyPrecision(2)).toBe(2);
    expect(qtyPrecision(3)).toBe(3);
  });

  it('缺失或非法值按整数 0 兜底（未选商品时按整数录入）', () => {
    expect(qtyPrecision(undefined)).toBe(0);
    expect(qtyPrecision(null)).toBe(0);
    expect(qtyPrecision(4)).toBe(0);
    expect(qtyPrecision(-1)).toBe(0);
  });

  it('步进：整数商品为 1，小数商品为 10 的负幂', () => {
    expect(qtyStep(0)).toBe(1);
    expect(qtyStep(1)).toBe(0.1);
    expect(qtyStep(2)).toBe(0.01);
    expect(qtyStep(3)).toBe(0.001);
    expect(qtyStep(undefined)).toBe(1);
  });

  it('切换商品精度时把已录数量归一化到新精度', () => {
    // 小数商品录入 1.25 后切成整数商品：必须落到整数，否则提交被后端精度校验拒绝
    expect(normalizeQty(1.25, 0)).toBe(1);
    expect(normalizeQty(1.5, 0)).toBe(2);
    expect(normalizeQty(1.25, 1)).toBe(1.3);
    expect(normalizeQty(1.25, 2)).toBe(1.25);
    expect(normalizeQty(1.2345, 3)).toBe(1.235);
  });

  it('归一化保留整数不变，缺失精度按整数处理', () => {
    expect(normalizeQty(5, 3)).toBe(5);
    expect(normalizeQty(5, undefined)).toBe(5);
    expect(normalizeQty('1.5', 0)).toBe(2);
    expect(normalizeQty(undefined, 2)).toBe(0);
    expect(normalizeQty(null, 2)).toBe(0);
  });
});

describe('格式化', () => {
  it('金额固定两位小数并加千分位', () => {
    expect(formatMoney(1234.5)).toBe('1,234.50');
    expect(formatMoney('1234.5')).toBe('1,234.50');
  });

  it('金额空值显示 0.00', () => {
    expect(formatMoney(undefined)).toBe('0.00');
    expect(formatMoney(null)).toBe('0.00');
  });

  it('数量固定三位小数', () => {
    expect(formatQty(5)).toBe('5.000');
  });

  it('数量去尾零：整数无小数位，小数保留有效位', () => {
    expect(formatQtyTrim(5)).toBe('5');
    expect(formatQtyTrim('1.500')).toBe('1.5');
    expect(formatQtyTrim(1.25)).toBe('1.25');
    expect(formatQtyTrim(0)).toBe('0');
    expect(formatQtyTrim(undefined)).toBe('0');
  });

  it('按商品精度格式化：整数不带小数位，N 位小数固定 N 位', () => {
    expect(formatQtyByScale(100, 0)).toBe('100');
    expect(formatQtyByScale(100, 2)).toBe('100.00');
    expect(formatQtyByScale(1.5, 1)).toBe('1.5');
    expect(formatQtyByScale(1.234, 3)).toBe('1.234');
    expect(formatQtyByScale(1234.5, 2)).toBe('1,234.50');
  });

  it('按商品精度格式化：按该精度四舍五入，空值按 0 补齐', () => {
    expect(formatQtyByScale(77.5, 0)).toBe('78');
    expect(formatQtyByScale('1.239', 2)).toBe('1.24');
    expect(formatQtyByScale(undefined, 2)).toBe('0.00');
    expect(formatQtyByScale(null, undefined)).toBe('0');
  });

  it('百分比固定两位小数带符号', () => {
    expect(formatPercent(68.244)).toBe('68.24%');
  });
});
