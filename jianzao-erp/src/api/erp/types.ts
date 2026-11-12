import type {
  ErpArApSourceType,
  ErpArApStatus,
  ErpBillStatus,
  ErpCostCategory,
  ErpCostType,
  ErpFlowType,
  ErpFreightAllocMethod,
  ErpOrderExecStatus,
  ErpOtherInReason,
  ErpPartyType,
  ErpProductType,
  ErpStockBizType,
  ErpTaxType
} from './enums';

export type {
  ErpArApSourceType,
  ErpArApStatus,
  ErpBillStatus,
  ErpCostCategory,
  ErpCostType,
  ErpFlowType,
  ErpFreightAllocMethod,
  ErpOrderExecStatus,
  ErpOtherInReason,
  ErpPartyType,
  ErpProductType,
  ErpStockBizType,
  ErpTaxType
};

/**
 * 后端 BigDecimal 以字符串序列化（Jackson ToStringSerializer），
 * Long 在超出 JS 安全整数时也会序列化为字符串，因此金额与 ID 一律放宽为联合类型。
 */
export type ErpAmount = string | number;
export type ErpId = string | number;

/** 分页响应 */
export interface ErpPageResult<T> {
  code: number;
  msg: string;
  rows: T[];
  total: number;
}

/* ------------------------------ 主数据 ------------------------------ */

export interface ErpProductVO {
  id: ErpId;
  code: string;
  name: string;
  spec?: string;
  unit: string;
  type: ErpProductType;
  /** 商品分类ID（空=未分类，DOC/26 §4） */
  categoryId?: ErpId | null;
  /** 商品分类名称（联查；分类已删除时为空，前端归「未分类」） */
  categoryName?: string | null;
  /** 商品分类排序（联查，折叠分组排序用） */
  categorySort?: number | null;
  /** 所属供应商ID */
  supplierId?: ErpId | null;
  /** 所属供应商名称（联查） */
  supplierName?: string | null;
  /** 数量精度位数 0~3（0=整数，DOC/26 §10） */
  qtyScale?: number | null;
  costPrice?: ErpAmount;
  purchasePrice?: ErpAmount;
  taxType?: ErpTaxType;
  freight?: ErpAmount;
  /** 库存下限（空=不启用预警，DOC/25） */
  minStock?: ErpAmount;
  /** 库存上限（空=不启用预警） */
  maxStock?: ErpAmount;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface ErpProductForm {
  id?: ErpId;
  code: string;
  name: string;
  spec?: string;
  unit: string;
  type: ErpProductType;
  categoryId?: ErpId | null;
  supplierId?: ErpId | null;
  /** 数量精度位数 0~3（0=整数，DOC/26 §10） */
  qtyScale?: number;
  costPrice?: number;
  purchasePrice?: number;
  taxType?: ErpTaxType;
  freight?: number;
  minStock?: number;
  maxStock?: number;
  status?: number;
  remark?: string;
}

/**
 * 商品资料表单字段（全字段可选）
 *
 * 作为 `ProductFormFields` 组件的 model 契约：商品资料弹窗与销售「现录现卖」共用同一份字段定义，
 * 避免两处表单各自维护、字段漂移。
 */
export interface ErpProductFormModel {
  code?: string | null;
  name?: string;
  spec?: string | null;
  unit?: string | null;
  type?: ErpProductType;
  categoryId?: ErpId | null;
  supplierId?: ErpId | null;
  qtyScale?: number | null;
  costPrice?: number | null;
  purchasePrice?: number | null;
  taxType?: ErpTaxType;
  freight?: number | null;
  minStock?: number | null;
  maxStock?: number | null;
  status?: number;
  remark?: string | null;
}

export interface ErpProductQuery extends PageQuery {
  keyword?: string;
  type?: ErpProductType | '';
  status?: number | '';
  categoryId?: ErpId | '';
  supplierId?: ErpId | '';
}

/** 商品分类（DOC/26 §4.2） */
export interface ErpProductCategoryVO {
  id: ErpId;
  code: string;
  name: string;
  sort?: number;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface ErpProductCategoryForm {
  id?: ErpId;
  code?: string;
  name: string;
  sort?: number;
  status?: number;
  remark?: string;
}

export interface ErpRecipeItem {
  materialId: ErpId;
  qtyPerUnit: number;
}

export interface ErpRecipeItemVO extends ErpRecipeItem {
  id: ErpId;
  productId: ErpId;
}

export interface ErpSupplierVO {
  id: ErpId;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  openingPayable?: ErpAmount;
  taxNo?: string;
  taxRate?: ErpAmount;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface ErpSupplierForm {
  id?: ErpId;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  openingPayable?: number;
  taxNo?: string;
  taxRate?: number;
  status?: number;
  remark?: string;
}

export interface ErpCustomerVO {
  id: ErpId;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  openingDebt?: ErpAmount;
  taxNo?: string;
  taxRate?: ErpAmount;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface ErpCustomerForm {
  id?: ErpId;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  openingDebt?: number;
  taxNo?: string;
  taxRate?: number;
  status?: number;
  remark?: string;
}

export interface ErpWarehouseVO {
  id: ErpId;
  code: string;
  name: string;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface ErpWarehouseForm {
  id?: ErpId;
  code: string;
  name: string;
  status?: number;
  remark?: string;
}

export interface ErpPartyQuery extends PageQuery {
  keyword?: string;
}

/* ------------------------------ 采购 ------------------------------ */

export interface ErpPurchaseItemVO {
  id?: ErpId;
  /** 来源采购订单行ID（转单生成时有值，直开单为空） */
  sourceOrderItemId?: ErpId;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  qty: ErpAmount;
  /** 数量精度位数 0~3（DOC/26 §10） */
  qtyScale?: number;
  price: ErpAmount;
  taxType?: ErpTaxType;
  taxRate?: ErpAmount;
  amount?: ErpAmount;
  freightAmount?: ErpAmount;
  freightPerUnit?: ErpAmount;
  costPrice?: ErpAmount;
  remark?: string;
}

export interface ErpPurchaseVO {
  id: ErpId;
  no?: string;
  /** 来源采购订单ID（转单生成时有值，直开单为空） */
  sourceOrderId?: ErpId;
  /** 来源采购订单号快照 */
  sourceOrderNo?: string;
  /** 供应商联系人快照 */
  contactName?: string;
  /** 供应商联系电话快照 */
  contactPhone?: string;
  supplierId: ErpId;
  supplierName?: string;
  bizDate: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  /** 发票类型（整单统一） */
  taxType?: ErpTaxType;
  /** 税率%（整单统一） */
  taxRate?: ErpAmount;
  goodsAmount?: ErpAmount;
  freightAmount?: ErpAmount;
  freightAllocMethod?: ErpFreightAllocMethod;
  totalAmount?: ErpAmount;
  remark?: string;
  createTime?: string;
  items: ErpPurchaseItemVO[];
}

export interface ErpPurchaseItemForm {
  productId: ErpId;
  /** 来源采购订单行ID（转单生成时回传，直开单为空） */
  sourceOrderItemId?: ErpId;
  qty: number;
  price: number;
  remark?: string;
}

export interface ErpPurchaseForm {
  id?: ErpId;
  /** 来源采购订单ID（转单生成时回传，直开单为空） */
  sourceOrderId?: ErpId;
  /** 来源采购订单号快照，仅回显 */
  sourceOrderNo?: string;
  /** 供应商联系人，转单时由后端从订单快照 */
  contactName?: string;
  /** 供应商联系电话，转单时由后端从订单快照 */
  contactPhone?: string;
  supplierId: ErpId;
  bizDate: string;
  /** 发票类型（整单统一），默认 VAT_INVOICE */
  taxType?: ErpTaxType;
  /** 税率%（整单统一），默认 13 */
  taxRate?: number;
  freightAmount?: number;
  remark?: string;
  confirm?: boolean;
  items: ErpPurchaseItemForm[];
}

export interface ErpPurchaseQuery extends PageQuery {
  keyword?: string;
  supplierId?: ErpId;
  status?: string;
}

/* ------------------------------ 生产 ------------------------------ */

/* ------------------------------ 销售 ------------------------------ */

export interface ErpSaleItemVO {
  id?: ErpId;
  /** 来源销售订单行ID（转单生成时有值，直开单为空） */
  sourceOrderItemId?: ErpId;
  productId: ErpId;
  /** 待建品（现录现卖）：草稿阶段 product_id=0，快照随明细返回 */
  newProduct?: ErpSaleNewProduct;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  qty: ErpAmount;
  /** 数量精度位数 0~3（DOC/26 §10） */
  qtyScale?: number;
  price: ErpAmount;
  taxType?: ErpTaxType;
  taxRate?: ErpAmount;
  amount?: ErpAmount;
  unitCost?: ErpAmount;
  costAmount?: ErpAmount;
  grossProfit?: ErpAmount;
  remark?: string;
}

export interface ErpSaleVO {
  id: ErpId;
  no?: string;
  /** 来源销售订单ID（转单生成时有值，直开单为空） */
  sourceOrderId?: ErpId;
  /** 来源销售订单号快照 */
  sourceOrderNo?: string;
  /** 客户联系人快照 */
  contactName?: string;
  /** 客户联系电话快照 */
  contactPhone?: string;
  /** 送货地址快照 */
  deliveryAddress?: string;
  customerId: ErpId;
  customerName?: string;
  bizDate: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  /** 发票类型（整单统一） */
  taxType?: ErpTaxType;
  /** 税率%（整单统一） */
  taxRate?: ErpAmount;
  /** 货品金额（不含运杂费） */
  goodsAmount?: ErpAmount;
  /** 运杂费总额 */
  freightAmount?: ErpAmount;
  freightAllocMethod?: ErpFreightAllocMethod;
  totalAmount?: ErpAmount;
  costAmount?: ErpAmount;
  grossProfit?: ErpAmount;
  grossMargin?: ErpAmount;
  remark?: string;
  createTime?: string;
  items: ErpSaleItemVO[];
  /** 确认时成本为 0 的商品名（软提示，不阻断确认） */
  costWarnings?: string[];
}

/** 现录现卖：随明细提交的新产品 */
export interface ErpSaleNewProduct extends ErpProductFormModel {
  name: string;
  /** 入库数量（默认等于销售数量，可改大；确认时按此数量入库，DOC/26 §8.3） */
  qty?: number;
}

export interface ErpSaleItemForm {
  productId?: ErpId;
  /** 来源销售订单行ID（转单生成时回传，直开单为空） */
  sourceOrderItemId?: ErpId;
  qty: number;
  newProduct?: ErpSaleNewProduct;
  price?: number;
  remark?: string;
}

export interface ErpSaleForm {
  id?: ErpId;
  /** 来源销售订单ID（转单生成时回传，直开单为空） */
  sourceOrderId?: ErpId;
  /** 来源销售订单号快照，仅回显 */
  sourceOrderNo?: string;
  /** 客户联系人，转单时由后端从订单快照 */
  contactName?: string;
  /** 客户联系电话，转单时由后端从订单快照 */
  contactPhone?: string;
  /** 送货地址，转单时由后端从订单快照 */
  deliveryAddress?: string;
  customerId: ErpId;
  bizDate: string;
  /** 发票类型（整单统一），默认 VAT_INVOICE */
  taxType?: ErpTaxType;
  /** 税率%（整单统一），默认 13 */
  taxRate?: number;
  /** 运杂费总额 */
  freightAmount?: number;
  remark?: string;
  confirm?: boolean;
  items: ErpSaleItemForm[];
}

export interface ErpSaleQuery extends PageQuery {
  keyword?: string;
  customerId?: ErpId;
  status?: string;
}

/* ------------------------------ 订单层（DOC/31） ------------------------------ */

/** 采购订单行（后端 ErpPurchaseOrderVo.ItemVo） */
export interface ErpPurchaseOrderItemVO {
  id: ErpId;
  seq?: number;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  /** 数量精度位数 0~3，前端据此设置数量输入精度 */
  qtyScale?: number;
  qty: ErpAmount;
  price: ErpAmount;
  taxType?: ErpTaxType;
  taxRate?: ErpAmount;
  amount?: ErpAmount;
  dueDate?: string;
  promisedDate?: string;
  executedQty?: ErpAmount;
  closedQty?: ErpAmount;
  /** 1=已关闭剩余 */
  closedFlag?: number;
  closeReason?: string;
  /** 未交量 = max(0, qty − executed − closed)，不落库 */
  remain?: ErpAmount;
  remark?: string;
}

/** 采购订单执行记录（详情页签 2）：入库单/退货单各一行 */
export interface ErpPurchaseOrderExecVO {
  id: ErpId;
  no?: string;
  /** RECEIPT=入库，RETURN=退货 */
  type: 'RECEIPT' | 'RETURN';
  typeText?: string;
  bizDate?: string;
  /** 该单挂到本订单行的数量合计 */
  qty?: ErpAmount;
  status?: ErpBillStatus;
  statusText?: string;
}

/** 采购订单头（后端 ErpPurchaseOrderVo） */
export interface ErpPurchaseOrderVO {
  id: ErpId;
  no?: string;
  supplierId: ErpId;
  supplierName?: string;
  bizDate: string;
  /** 派生交期：最早未交行交期，recalc 覆盖写 */
  dueDate?: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  execStatus: ErpOrderExecStatus;
  /** 执行进度中文文案（后端回填） */
  execStatusText?: string;
  /** 供应商单号（对方单号） */
  supplierOrderNo?: string;
  buyerId?: ErpId;
  buyerName?: string;
  contactName?: string;
  contactPhone?: string;
  allowDelayDays?: number;
  overTolPct?: ErpAmount;
  taxType?: ErpTaxType;
  taxRate?: ErpAmount;
  /** 订单金额（不含运杂费）=Σ行 amount */
  goodsAmount?: ErpAmount;
  remark?: string;
  confirmedAt?: string;
  withdrawnAt?: string;
  voidedAt?: string;
  createTime?: string;
  /** 订单量合计 */
  totalQty?: ErpAmount;
  /** 已执行量合计（净额） */
  executedQty?: ErpAmount;
  /** 已关闭量合计 */
  closedQty?: ErpAmount;
  /** 未交量合计，负数截断为 0 */
  remainQty?: ErpAmount;
  items?: ErpPurchaseOrderItemVO[];
}

export interface ErpPurchaseOrderQuery extends PageQuery {
  /** 订单号/供应商单号 */
  keyword?: string;
  supplierId?: ErpId;
  status?: string;
  execStatus?: ErpOrderExecStatus;
  /** 只看未完成（CONFIRMED 且 NOT_STARTED/PARTIAL） */
  unfinished?: boolean;
  /** 只看已逾期 */
  overdue?: boolean;
  dueDateStart?: string;
  dueDateEnd?: string;
}

export interface ErpPurchaseOrderItemForm {
  /** 已确认订单改单时必填，且须属于本单 */
  id?: ErpId;
  productId: ErpId;
  qty: number;
  price: number;
  dueDate: string;
  promisedDate?: string;
  remark?: string;
}

export interface ErpPurchaseOrderForm {
  id?: ErpId;
  supplierId: ErpId;
  bizDate: string;
  supplierOrderNo?: string;
  buyerId?: ErpId;
  buyerName?: string;
  contactName?: string;
  contactPhone?: string;
  allowDelayDays?: number;
  /** 超收容差 %，M1 前端只读 */
  overTolPct?: number;
  taxType?: ErpTaxType;
  taxRate?: number;
  remark?: string;
  /** 已确认订单改单时必填（qty/due_date/容差等），见 DOC/31 §6.3 */
  reason?: string;
  items: ErpPurchaseOrderItemForm[];
}

/** 订单转采购入库单入参；不传 items 默认带全部未交行、数量＝remain */
export interface ErpPurchaseOrderReceiptForm {
  bizDate?: string;
  remark?: string;
  items?: ErpPurchaseOrderReceiptItemForm[];
}

export interface ErpPurchaseOrderReceiptItemForm {
  orderItemId: ErpId;
  qty: number;
}

/** 销售订单行（后端 ErpSaleOrderVo.ItemVo） */
export interface ErpSaleOrderItemVO {
  id: ErpId;
  seq?: number;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  /** 数量精度位数 0~3，前端据此设置数量输入精度 */
  qtyScale?: number;
  qty: ErpAmount;
  price: ErpAmount;
  taxType?: ErpTaxType;
  taxRate?: ErpAmount;
  amount?: ErpAmount;
  dueDate?: string;
  promisedDate?: string;
  executedQty?: ErpAmount;
  closedQty?: ErpAmount;
  /** 1=已关闭剩余 */
  closedFlag?: number;
  closeReason?: string;
  /** 未交量 = max(0, qty − executed − closed)，不落库 */
  remain?: ErpAmount;
  remark?: string;
}

/** 销售订单执行记录（详情页签 2）：出库单/退货单各一行 */
export interface ErpSaleOrderExecVO {
  id: ErpId;
  no?: string;
  /** DELIVERY=出库，RETURN=退货 */
  type: 'DELIVERY' | 'RETURN';
  typeText?: string;
  bizDate?: string;
  /** 该单挂到本订单行的数量合计 */
  qty?: ErpAmount;
  status?: ErpBillStatus;
  statusText?: string;
}

/** 销售订单头（后端 ErpSaleOrderVo） */
export interface ErpSaleOrderVO {
  id: ErpId;
  no?: string;
  customerId: ErpId;
  customerName?: string;
  bizDate: string;
  /** 派生交期：最早未交行交期，recalc 覆盖写 */
  dueDate?: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  execStatus: ErpOrderExecStatus;
  /** 执行进度中文文案（后端回填） */
  execStatusText?: string;
  /** 客户单号（对方单号） */
  customerOrderNo?: string;
  salespersonId?: ErpId;
  salespersonName?: string;
  contactName?: string;
  contactPhone?: string;
  deliveryAddress?: string;
  allowDelayDays?: number;
  overTolPct?: ErpAmount;
  taxType?: ErpTaxType;
  taxRate?: ErpAmount;
  /** 订单金额（不含运杂费）=Σ行 amount */
  goodsAmount?: ErpAmount;
  remark?: string;
  confirmedAt?: string;
  withdrawnAt?: string;
  voidedAt?: string;
  createTime?: string;
  /** 订单量合计 */
  totalQty?: ErpAmount;
  /** 已执行量合计（净额） */
  executedQty?: ErpAmount;
  /** 已关闭量合计 */
  closedQty?: ErpAmount;
  /** 未交量合计，负数截断为 0 */
  remainQty?: ErpAmount;
  items?: ErpSaleOrderItemVO[];
}

export interface ErpSaleOrderQuery extends PageQuery {
  /** 订单号/客户单号 */
  keyword?: string;
  customerId?: ErpId;
  status?: string;
  execStatus?: ErpOrderExecStatus;
  /** 只看未完成（CONFIRMED 且 NOT_STARTED/PARTIAL） */
  unfinished?: boolean;
  /** 只看已逾期 */
  overdue?: boolean;
  dueDateStart?: string;
  dueDateEnd?: string;
}

export interface ErpSaleOrderItemForm {
  /** 已确认订单改单时必填，且须属于本单 */
  id?: ErpId;
  productId: ErpId;
  qty: number;
  price: number;
  dueDate: string;
  promisedDate?: string;
  remark?: string;
}

export interface ErpSaleOrderForm {
  id?: ErpId;
  customerId: ErpId;
  bizDate: string;
  customerOrderNo?: string;
  salespersonId?: ErpId;
  salespersonName?: string;
  contactName?: string;
  contactPhone?: string;
  deliveryAddress?: string;
  allowDelayDays?: number;
  /** 超发容差 %，M1 前端只读 */
  overTolPct?: number;
  taxType?: ErpTaxType;
  taxRate?: number;
  remark?: string;
  /** 已确认订单改单时必填（qty/due_date/容差/送货地址等），见 DOC/31 §6.3 */
  reason?: string;
  items: ErpSaleOrderItemForm[];
}

/** 订单转销售出库单入参；不传 items 默认带全部未交行、数量＝remain */
export interface ErpSaleOrderDeliveryForm {
  bizDate?: string;
  remark?: string;
  items?: ErpSaleOrderDeliveryItemForm[];
}

export interface ErpSaleOrderDeliveryItemForm {
  orderItemId: ErpId;
  qty: number;
}

/** 订单变更留痕 biz_order_change_log（只增不删，DOC/31 §5.5.4） */
export interface ErpOrderChangeLogVO {
  id: ErpId;
  /** PURCHASE/SALE */
  orderType: 'PURCHASE' | 'SALE';
  orderId: ErpId;
  orderNo?: string;
  /** 头级变更为空 */
  itemId?: ErpId;
  /** qty/price/due_date/.../status */
  fieldKey?: string;
  oldValue?: string;
  newValue?: string;
  reason?: string;
  operatorId?: ErpId;
  operatorName?: string;
  operateTime?: string;
}

/* ------------------------------ 其他出库 ------------------------------ */

export interface ErpOtherOutItemVO {
  id?: ErpId;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  qty: ErpAmount;
  /** 数量精度位数 0~3（DOC/26 §10） */
  qtyScale?: number;
  unitCost?: ErpAmount;
  amount?: ErpAmount;
  remark?: string;
}

export interface ErpOtherOutVO {
  id: ErpId;
  no?: string;
  bizDate: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  costCategory: ErpCostCategory;
  costAmount?: ErpAmount;
  remark?: string;
  createTime?: string;
  items: ErpOtherOutItemVO[];
}

export interface ErpOtherOutItemForm {
  productId: ErpId;
  qty: number;
  remark?: string;
}

export interface ErpOtherOutForm {
  id?: ErpId;
  bizDate: string;
  costCategory: ErpCostCategory;
  remark?: string;
  confirm?: boolean;
  items: ErpOtherOutItemForm[];
}

export interface ErpOtherOutQuery extends PageQuery {
  keyword?: string;
  costCategory?: string;
  status?: string;
}

/* ------------------------------ 其他入库 ------------------------------ */

export interface ErpOtherInItemVO {
  id?: ErpId;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  qty: ErpAmount;
  /** 数量精度位数 0~3（DOC/26 §10） */
  qtyScale?: number;
  unitCost?: ErpAmount;
  amount?: ErpAmount;
  remark?: string;
}

export interface ErpOtherInVO {
  id: ErpId;
  no?: string;
  bizDate: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  reason: ErpOtherInReason;
  amount?: ErpAmount;
  remark?: string;
  createTime?: string;
  items: ErpOtherInItemVO[];
}

export interface ErpOtherInItemForm {
  productId: ErpId;
  qty: number;
  unitCost: number;
  remark?: string;
}

export interface ErpOtherInForm {
  id?: ErpId;
  bizDate: string;
  reason: ErpOtherInReason;
  remark?: string;
  confirm?: boolean;
  items: ErpOtherInItemForm[];
}

export interface ErpOtherInQuery extends PageQuery {
  keyword?: string;
  reason?: string;
  status?: string;
}

/* ------------------------------ 库存 ------------------------------ */

export interface ErpStockVO {
  productId: ErpId;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  type: ErpProductType;
  warehouseId?: ErpId;
  qty: ErpAmount;
  avgCost?: ErpAmount;
  stockAmount?: ErpAmount;
  /** 成本缺失：有库存但均价为 0（DOC/26 §2.4 防线③） */
  costMissing?: boolean;
}

export interface ErpStockLogVO {
  id: ErpId;
  bizType: ErpStockBizType;
  bizNo?: string;
  bizId?: ErpId;
  productId: ErpId;
  warehouseId?: ErpId;
  qty: ErpAmount;
  unitCost?: ErpAmount;
  amount?: ErpAmount;
  partnerType?: string;
  partnerId?: ErpId;
  partnerName?: string;
  balanceAfter?: ErpAmount;
  bizDate?: string;
  seq?: number;
}

export interface ErpStockQuery {
  keyword?: string;
  type?: ErpProductType;
}

export interface StOpeningVO {
  id: ErpId;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  warehouseId: ErpId;
  qty: ErpAmount;
  /** 数量精度位数 0~3（DOC/26 §10） */
  qtyScale?: number;
  unitCost?: ErpAmount;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  createTime?: string;
}

export interface StOpeningForm {
  productId: ErpId;
  warehouseId: ErpId;
  qty: number;
  unitCost: number;
}

/* ------------------------------ 财务 ------------------------------ */

export interface FinArApVO {
  id: ErpId;
  partyType: ErpPartyType;
  partyId: ErpId;
  partyName?: string;
  sourceType: ErpArApSourceType;
  sourceId?: ErpId;
  sourceNo?: string;
  bizDate?: string;
  amount: ErpAmount;
  settledAmount: ErpAmount;
  unsettledAmount: ErpAmount;
  status: ErpArApStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
}

export interface FinArApQuery extends PageQuery {
  partyType?: ErpPartyType;
  partyId?: ErpId;
}

export interface FinArApPartySummary {
  partyId: ErpId;
  partyName?: string;
  amount: ErpAmount;
  settled: ErpAmount;
  unsettled: ErpAmount;
  opening: ErpAmount;
}

export interface FinArApSummaryVO {
  partyType: ErpPartyType;
  totalAmount: ErpAmount;
  settledAmount: ErpAmount;
  unsettledAmount: ErpAmount;
  openingAmount: ErpAmount;
  parties: FinArApPartySummary[];
}

export interface FinOpeningVO {
  id: ErpId;
  partyType: ErpPartyType;
  partyId: ErpId;
  partyName?: string;
  amount: ErpAmount;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  remark?: string;
  createTime?: string;
}

export interface FinOpeningForm {
  partyType: ErpPartyType;
  partyId: ErpId;
  amount: number;
  remark?: string;
}

export interface FinReceiptAllocVO {
  id?: ErpId;
  receiptId?: ErpId;
  arApId: ErpId;
  amount: ErpAmount;
}

export interface FinReceiptVO {
  id: ErpId;
  no?: string;
  customerId: ErpId;
  customerName?: string;
  amount: ErpAmount;
  bizDate: string;
  account?: string;
  /** 结算账户ID（存量单为空） */
  accountId?: ErpId;
  remark?: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  createTime?: string;
  allocations: FinReceiptAllocVO[];
}

export interface FinAllocationForm {
  arApId: ErpId;
  amount: number;
}

export interface FinReceiptForm {
  id?: ErpId;
  customerId: ErpId;
  bizDate: string;
  amount: number;
  account?: string;
  /** 结算账户ID（新增必填） */
  accountId?: ErpId;
  remark?: string;
  confirm?: boolean;
  allocations: FinAllocationForm[];
}

export interface FinReceiptQuery extends PageQuery {
  customerId?: ErpId;
  status?: string;
}

export interface FinPaymentAllocVO {
  id?: ErpId;
  paymentId?: ErpId;
  arApId: ErpId;
  amount: ErpAmount;
}

export interface FinPaymentVO {
  id: ErpId;
  no?: string;
  supplierId: ErpId;
  supplierName?: string;
  amount: ErpAmount;
  bizDate: string;
  account?: string;
  /** 结算账户ID（存量单为空） */
  accountId?: ErpId;
  remark?: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  createTime?: string;
  allocations: FinPaymentAllocVO[];
}

export interface FinPaymentForm {
  id?: ErpId;
  supplierId: ErpId;
  bizDate: string;
  amount: number;
  account?: string;
  /** 结算账户ID（新增必填） */
  accountId?: ErpId;
  remark?: string;
  confirm?: boolean;
  allocations: FinAllocationForm[];
}

export interface FinPaymentQuery extends PageQuery {
  supplierId?: ErpId;
  status?: string;
}

export interface FinOtherFlowVO {
  id: ErpId;
  no?: string;
  flowType: ErpFlowType;
  category?: string;
  amount: ErpAmount;
  bizDate: string;
  account?: string;
  /** 结算账户ID（存量单为空） */
  accountId?: ErpId;
  affectCost?: number;
  remark?: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  createTime?: string;
}

export interface FinOtherFlowForm {
  id?: ErpId;
  flowType: ErpFlowType;
  category?: string;
  amount: number;
  bizDate: string;
  account?: string;
  /** 结算账户ID（新增必填） */
  accountId?: ErpId;
  affectCost?: number;
  remark?: string;
  confirm?: boolean;
}

export interface FinOtherFlowQuery extends PageQuery {
  flowType?: ErpFlowType;
  status?: string;
}

/* ------------------------------ 对账单 ------------------------------ */

/**
 * 对账单由服务端渲染为 HTML（GET /erp/print/STATEMENT），
 * 前端以 iframe 加载后打印，不解析 JSON。
 */

/* ------------------------------ 工作台 ------------------------------ */

/** 工作台草稿待办（各类单据 DRAFT 数量） */
export type ErpPartnerType = 'CUSTOMER' | 'SUPPLIER';

/** 往来单位当月明细单据 */
/* ------------------------------ 打印模板 ------------------------------ */

/** 打印场景 */
export type ErpPrintScene = 'SALE_DELIVERY' | 'STATEMENT' | 'PURCHASE_IN' | 'PRODUCTION' | 'RECEIPT' | 'PAYMENT';

/** 打印模板 */

/* ------------------------------ 打印模板 ------------------------------ */

export interface PrintTemplateVO {
  id: ErpId;
  /** 模板编码 */
  code?: string;
  /** 模板名称 */
  name: string;
  /** 适用场景 */
  scene: ErpPrintScene;
  /** 专属客户（为空表示通用模板） */
  customerId?: ErpId;
  customerName?: string;
  /** 渲染引擎 */
  engine?: string;
  /** 模板内容（HTML + Thymeleaf 表达式） */
  content?: string;
  /** 纸张规格 */
  paper?: string;
  /** 版本号，编辑递增 */
  version?: number;
  /** 是否默认模板 1/0 */
  isDefault?: number;
  /** 状态 1 启用 / 0 停用 */
  status?: number;
  /** 变量说明 JSON */
  variablesJson?: string;
  createTime?: string;
}

/** 打印模板保存表单 */
export interface PrintTemplateForm {
  id?: ErpId;
  code?: string;
  name: string;
  scene: ErpPrintScene;
  content: string;
  paper?: string;
  /** 专属客户，留空为通用模板 */
  customerId?: ErpId;
  isDefault?: number;
  status?: number;
  variablesJson?: string;
}

export interface FinCostReportVO {
  month: string;
  salesAmount: ErpAmount;
  /** 销售成本 = 已确认销售单 cost_amount 合计 */
  salesCost: ErpAmount;
  /** 生产材料成本（参考，不计入综合成本） */
  productionMaterialCost: ErpAmount;
  otherOutCost: ErpAmount;
  otherExpenseCost: ErpAmount;
  totalCost: ErpAmount;
  grossProfit: ErpAmount;
  /** 毛利率百分数（后端计算，销售为 0 时为 null） */
  grossMargin?: ErpAmount;
  /** 库存金额参考值 = Σ 库存数量 × 移动加权均价（不计入本期损益） */
  stockAmount?: ErpAmount;
  /** 本期成本缺失的销售明细行数（成本金额为 0，DOC/26 §2.4 防线③） */
  costMissingCount?: number;
}

/* ------------------------------ 结算账户（DOC/25） ------------------------------ */

export type ErpAccountType = 'CASH' | 'BANK' | 'WECHAT' | 'ALIPAY' | 'OTHER';

export interface FinAccountVO {
  id: ErpId;
  code: string;
  name: string;
  type: ErpAccountType;
  openingBalance?: ErpAmount;
  status: number;
  remark?: string;
  balance?: ErpAmount;
  createTime?: string;
}

export interface FinAccountForm {
  id?: ErpId;
  code: string;
  name: string;
  type: ErpAccountType;
  openingBalance?: number;
  status: number;
  remark?: string;
}

export interface FinAccountQuery extends PageQuery {
  keyword?: string;
  type?: string;
  status?: number;
}

export interface FinAccountFlowVO {
  bizType: 'RECEIPT' | 'PAYMENT' | 'OTHER_FLOW';
  bizNo?: string;
  bizDate: string;
  amount: ErpAmount;
  direction: 'IN' | 'OUT';
  counterparty?: string;
  rowId: ErpId;
}

/* ------------------------------ 库存预警 / 最近成交价（DOC/25） ------------------------------ */

export interface ErpStockAlertVO {
  productId: ErpId;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  minStock?: ErpAmount;
  maxStock?: ErpAmount;
  totalQty: ErpAmount;
  gap: ErpAmount;
  alertType: 'LOW' | 'HIGH';
}

export interface ErpSaleLastPriceVO {
  price: ErpAmount;
  bizDate: string;
  saleNo?: string;
  scope: 'CUSTOMER' | 'MARKET' | 'NONE';
}

/** 历史价格（DOC/26 §7）：按商品维度查询，行上带客户名/供应商名 */
export interface ErpPriceHistoryVO {
  bizDate: string;
  orderNo?: string;
  partyName?: string;
  price: ErpAmount;
  qty: ErpAmount;
  amount: ErpAmount;
}

/* ------------------------------ 盘点单（DOC/23） ------------------------------ */

/* ------------------------------ 退货单（DOC/24） ------------------------------ */

export interface SaleReturnItemVO {
  id?: ErpId;
  saleItemId: ErpId;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  qty: number;
  /** 数量精度位数 0~3（DOC/26 §10） */
  qtyScale?: number;
  price: number;
  amount?: ErpAmount;
  unitCost?: ErpAmount;
  returnCost?: ErpAmount;
  remark?: string;
  originalQty?: ErpAmount;
  returnedQty?: ErpAmount;
  returnableQty?: ErpAmount;
}

export interface SaleReturnVO {
  id: ErpId;
  no?: string;
  saleId: ErpId;
  saleNo?: string;
  customerId: ErpId;
  customerName?: string;
  bizDate: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  totalAmount?: ErpAmount;
  returnCostAmount?: ErpAmount;
  needRefund?: number;
  refundAmount?: ErpAmount;
  refundReceiptId?: ErpId;
  remark?: string;
  createTime?: string;
  items: SaleReturnItemVO[];
}

export interface SaleReturnForm {
  id?: ErpId;
  saleId: ErpId;
  bizDate: string;
  needRefund?: boolean;
  refundAmount?: number;
  refundAccountId?: ErpId;
  remark?: string;
  confirm?: boolean;
  items: { saleItemId: ErpId; qty: number; price: number; remark?: string }[];
}

export interface SaleReturnQuery extends PageQuery {
  keyword?: string;
  customerId?: ErpId;
  status?: string;
}

export interface PurchaseReturnItemVO {
  id?: ErpId;
  purchaseItemId: ErpId;
  productId: ErpId;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  qty: number;
  /** 数量精度位数 0~3（DOC/26 §10） */
  qtyScale?: number;
  price: number;
  amount?: ErpAmount;
  unitCost?: ErpAmount;
  returnCost?: ErpAmount;
  remark?: string;
  originalQty?: ErpAmount;
  returnedQty?: ErpAmount;
  returnableQty?: ErpAmount;
}

export interface PurchaseReturnVO {
  id: ErpId;
  no?: string;
  purchaseId: ErpId;
  purchaseNo?: string;
  supplierId: ErpId;
  supplierName?: string;
  bizDate: string;
  status: ErpBillStatus;
  /** 状态中文文案（后端回填，见 org.erp.common.enums.StatusText） */
  statusText?: string;
  totalAmount?: ErpAmount;
  returnCostAmount?: ErpAmount;
  remark?: string;
  createTime?: string;
  items: PurchaseReturnItemVO[];
}

export interface PurchaseReturnForm {
  id?: ErpId;
  purchaseId: ErpId;
  bizDate: string;
  remark?: string;
  confirm?: boolean;
  items: { purchaseItemId: ErpId; qty: number; price: number; remark?: string }[];
}

export interface PurchaseReturnQuery extends PageQuery {
  keyword?: string;
  supplierId?: ErpId;
  status?: string;
}

/* ------------------------------ 委外加工 ------------------------------ */

