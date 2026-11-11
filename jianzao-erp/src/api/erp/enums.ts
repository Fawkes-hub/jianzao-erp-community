/**
 * ERP 业务枚举 —— 与后端 org.erp.* 的字符串取值严格一致
 * 后端取值来源：04-erp-schema.sql 的 column comment
 */

/** 单据状态 */
export type ErpBillStatus = 'DRAFT' | 'CONFIRMED' | 'VOID';

/** 订单执行进度（biz_purchase_order/biz_sale_order 的 exec_status，DOC/31 §6.1.1） */
export type ErpOrderExecStatus = 'NOT_STARTED' | 'PARTIAL' | 'FULL' | 'CLOSED';

/** 应收应付台账状态 */
export type ErpArApStatus = 'OPEN' | 'PARTIAL' | 'SETTLED' | 'VOID';

/** 产品类型 */
export type ErpProductType = 'PRODUCT' | 'MATERIAL';

/** 发票类型 */
export type ErpTaxType = 'NO_TAX' | 'VAT_INVOICE' | 'GENERAL_INVOICE';

/** 往来单位类型 */
export type ErpPartyType = 'AR' | 'AP';

/** 往来台账来源 */
export type ErpArApSourceType = 'SALE' | 'PURCHASE' | 'OPENING';

/** 运杂费分摊方式 */
export type ErpFreightAllocMethod = 'BY_AMOUNT' | 'BY_QTY' | 'MANUAL';

/** 其他出库成本分类 */
export type ErpCostCategory = 'PRODUCTION_CONSUMABLE' | 'OFFICE' | 'MAINTENANCE' | 'OTHER';

/** 其他收支类型 */
export type ErpFlowType = 'INCOME' | 'EXPENSE';

/** 成本台账类型 */
export type ErpCostType = 'PRODUCTION_MATERIAL' | 'OTHER_OUT' | 'OTHER_EXPENSE';

/** 其他入库原因 */
export type ErpOtherInReason = 'STOCKTAKING_IN' | 'REWORK_IN' | 'GIFT_IN' | 'TRANSFER_IN' | 'RETURN_IN' | 'OTHER';

/** 结算账户类型 */
export type ErpAccountType = 'CASH' | 'BANK' | 'WECHAT' | 'ALIPAY' | 'OTHER';

/** 库存预警类型 */
export type ErpStockAlertType = 'LOW' | 'HIGH';

/** 库存流水业务类型 */
export type ErpStockBizType =
  | 'OPENING'
  | 'PURCHASE'
  | 'PROD_IN'
  | 'PROD_OUT'
  | 'SALE'
  | 'OTHER_OUT'
  | 'OTHER_IN'
  | 'CHECK'
  | 'SALE_RETURN'
  | 'PURCHASE_RETURN'
  | 'QUICK_IN'
  | 'VOID';

export const ERP_BILL_STATUS: Record<ErpBillStatus, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  CONFIRMED: { label: '已确认', type: 'success' },
  VOID: { label: '已作废', type: 'danger' }
};

/** 订单执行进度文案与配色，与后端 org.erp.common.enums.StatusText.EXEC 一一对应 */
export const ERP_ORDER_EXEC_STATUS: Record<ErpOrderExecStatus, { label: string; type: string }> = {
  NOT_STARTED: { label: '未执行', type: 'info' },
  PARTIAL: { label: '部分执行', type: 'warning' },
  FULL: { label: '已完成', type: 'success' },
  CLOSED: { label: '已关闭', type: 'primary' }
};

export const ERP_AR_AP_STATUS: Record<ErpArApStatus, { label: string; type: string }> = {
  OPEN: { label: '未核销', type: 'warning' },
  PARTIAL: { label: '部分核销', type: 'primary' },
  SETTLED: { label: '已核销', type: 'success' },
  VOID: { label: '已冲销', type: 'info' }
};

export const ERP_PRODUCT_TYPE: Record<ErpProductType, { label: string; type: string }> = {
  PRODUCT: { label: '成品', type: 'primary' },
  MATERIAL: { label: '原料', type: 'warning' }
};

export const ERP_TAX_TYPE: Record<ErpTaxType, { label: string }> = {
  NO_TAX: { label: '不含税' },
  VAT_INVOICE: { label: '增值税发票' },
  GENERAL_INVOICE: { label: '普通发票' }
};

/** 商品数量精度可选值（md_product.qty_scale，0=整数，DOC/26 §10） */
export const ERP_QTY_SCALE_OPTIONS: { value: number; label: string }[] = [
  { value: 0, label: '整数' },
  { value: 1, label: '1位小数' },
  { value: 2, label: '2位小数' },
  { value: 3, label: '3位小数' }
];

export const ERP_PARTY_TYPE: Record<ErpPartyType, { label: string }> = {
  AR: { label: '应收' },
  AP: { label: '应付' }
};

export const ERP_AR_AP_SOURCE_TYPE: Record<ErpArApSourceType, { label: string }> = {
  SALE: { label: '销售出库' },
  PURCHASE: { label: '采购入库' },
  OPENING: { label: '期初' }
};

export const ERP_FREIGHT_ALLOC_METHOD: Record<ErpFreightAllocMethod, { label: string }> = {
  BY_AMOUNT: { label: '按金额' },
  BY_QTY: { label: '按数量' },
  MANUAL: { label: '手工' }
};

export const ERP_COST_CATEGORY: Record<ErpCostCategory, { label: string }> = {
  PRODUCTION_CONSUMABLE: { label: '生产消耗' },
  OFFICE: { label: '办公' },
  MAINTENANCE: { label: '维修' },
  OTHER: { label: '其他' }
};

export const ERP_FLOW_TYPE: Record<ErpFlowType, { label: string; type: string }> = {
  INCOME: { label: '其他收入', type: 'success' },
  EXPENSE: { label: '其他支出', type: 'danger' }
};

export const ERP_COST_TYPE: Record<ErpCostType, { label: string }> = {
  PRODUCTION_MATERIAL: { label: '生产材料成本' },
  OTHER_OUT: { label: '其他出库成本' },
  OTHER_EXPENSE: { label: '计入成本的其他支出' }
};

export const ERP_OTHER_IN_REASON: Record<ErpOtherInReason, { label: string }> = {
  STOCKTAKING_IN: { label: '盘盈' },
  REWORK_IN: { label: '返工入库' },
  GIFT_IN: { label: '赠品调入' },
  TRANSFER_IN: { label: '调拨入' },
  RETURN_IN: { label: '退货收回' },
  OTHER: { label: '其他' }
};

export const ERP_STOCK_BIZ_TYPE: Record<ErpStockBizType, { label: string; type: string }> = {
  OPENING: { label: '期初', type: 'info' },
  PURCHASE: { label: '采购入库', type: 'success' },
  PROD_IN: { label: '生产入库', type: 'success' },
  PROD_OUT: { label: '生产领料', type: 'warning' },
  SALE: { label: '销售出库', type: 'primary' },
  OTHER_OUT: { label: '其他出库', type: 'warning' },
  OTHER_IN: { label: '其他入库', type: 'success' },
  CHECK: { label: '盘点', type: 'warning' },
  SALE_RETURN: { label: '销售退货', type: 'danger' },
  PURCHASE_RETURN: { label: '采购退货', type: 'danger' },
  QUICK_IN: { label: '现录现卖入库', type: 'success' },
  VOID: { label: '撤回冲销', type: 'info' }
};

export const ERP_ACCOUNT_TYPE: Record<ErpAccountType, { label: string }> = {
  CASH: { label: '现金' },
  BANK: { label: '银行' },
  WECHAT: { label: '微信' },
  ALIPAY: { label: '支付宝' },
  OTHER: { label: '其他' }
};

export const ERP_STOCK_ALERT_TYPE: Record<ErpStockAlertType, { label: string; type: string }> = {
  LOW: { label: '低于下限', type: 'danger' },
  HIGH: { label: '高于上限', type: 'warning' }
};
