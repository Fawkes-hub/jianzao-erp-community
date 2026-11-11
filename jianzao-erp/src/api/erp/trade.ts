import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type {
  ErpOtherInForm,
  ErpOtherInQuery,
  ErpOtherInVO,
  ErpOtherOutForm,
  ErpOtherOutQuery,
  ErpOtherOutVO,
  ErpPriceHistoryVO,
  ErpPurchaseForm,
  ErpPurchaseItemVO,
  ErpPurchaseQuery,
  ErpPurchaseVO,
  ErpSaleForm,
  ErpSaleLastPriceVO,
  ErpSaleQuery,
  ErpSaleVO,
  PurchaseReturnForm,
  PurchaseReturnItemVO,
  PurchaseReturnQuery,
  PurchaseReturnVO,
  SaleReturnForm,
  SaleReturnItemVO,
  SaleReturnQuery,
  SaleReturnVO
} from './types';

/* ------------------------------ 采购 ------------------------------ */

export function listPurchase(query: ErpPurchaseQuery): AxiosPromise<ErpPurchaseVO[]> {
  return request({ url: '/erp/purchase/orders/list', method: 'get', params: query });
}

export function getPurchase(id: string | number): AxiosPromise<ErpPurchaseVO> {
  return request({ url: `/erp/purchase/orders/${id}`, method: 'get' });
}

/** 预览运杂费分摊与成本单价，不落库 */
export function previewAllocation(data: ErpPurchaseForm): AxiosPromise<ErpPurchaseItemVO[]> {
  return request({ url: '/erp/purchase/orders/preview-allocation', method: 'post', data });
}

export function addPurchase(data: ErpPurchaseForm): AxiosPromise<ErpPurchaseVO> {
  return request({ url: '/erp/purchase/orders', method: 'post', data });
}

export function updatePurchase(data: ErpPurchaseForm): AxiosPromise<ErpPurchaseVO> {
  return request({ url: `/erp/purchase/orders/${data.id}`, method: 'put', data });
}

export function confirmPurchase(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase/orders/${id}/confirm`, method: 'post' });
}

export function withdrawPurchase(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase/orders/${id}/withdraw`, method: 'post' });
}

export function voidPurchase(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase/orders/${id}/void`, method: 'post' });
}

/** 删除草稿采购单（逻辑删除，单号不回收重用） */
export function delPurchase(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase/orders/${id}`, method: 'delete' });
}

/** 该商品的历史采购价（DOC/26 §7），不限供应商按业务日期倒序，行上带供应商名 */
export function getPurchasePriceHistory(productId: string | number, limit = 10): AxiosPromise<ErpPriceHistoryVO[]> {
  return request({ url: '/erp/purchase/orders/prices/history', method: 'get', params: { productId, limit } });
}

/* ------------------------------ 销售 ------------------------------ */

export function listSale(query: ErpSaleQuery): AxiosPromise<ErpSaleVO[]> {
  return request({ url: '/erp/sales/orders/list', method: 'get', params: query });
}

export function getSale(id: string | number): AxiosPromise<ErpSaleVO> {
  return request({ url: `/erp/sales/orders/${id}`, method: 'get' });
}

export function addSale(data: ErpSaleForm): AxiosPromise<ErpSaleVO> {
  return request({ url: '/erp/sales/orders', method: 'post', data });
}

export function updateSale(data: ErpSaleForm): AxiosPromise<ErpSaleVO> {
  return request({ url: `/erp/sales/orders/${data.id}`, method: 'put', data });
}

export function confirmSale(id: string | number): AxiosPromise<ErpSaleVO> {
  return request({ url: `/erp/sales/orders/${id}/confirm`, method: 'post' });
}

export function withdrawSale(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/sales/orders/${id}/withdraw`, method: 'post' });
}

export function voidSale(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/sales/orders/${id}/void`, method: 'post' });
}

/** 删除草稿销售单（逻辑删除，单号不回收重用） */
export function delSale(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/sales/orders/${id}`, method: 'delete' });
}

/* ------------------------------ 其他出库 ------------------------------ */

export function listOtherOut(query: ErpOtherOutQuery): AxiosPromise<ErpOtherOutVO[]> {
  return request({ url: '/erp/other-out/orders/list', method: 'get', params: query });
}

export function getOtherOut(id: string | number): AxiosPromise<ErpOtherOutVO> {
  return request({ url: `/erp/other-out/orders/${id}`, method: 'get' });
}

export function addOtherOut(data: ErpOtherOutForm): AxiosPromise<ErpOtherOutVO> {
  return request({ url: '/erp/other-out/orders', method: 'post', data });
}

export function updateOtherOut(data: ErpOtherOutForm): AxiosPromise<ErpOtherOutVO> {
  return request({ url: `/erp/other-out/orders/${data.id}`, method: 'put', data });
}

export function confirmOtherOut(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-out/orders/${id}/confirm`, method: 'post' });
}

export function withdrawOtherOut(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-out/orders/${id}/withdraw`, method: 'post' });
}

export function voidOtherOut(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-out/orders/${id}/void`, method: 'post' });
}

/** 删除草稿其他出库单（逻辑删除，单号不回收重用） */
export function delOtherOut(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-out/orders/${id}`, method: 'delete' });
}

/* ------------------------------ 其他入库 ------------------------------ */

export function listOtherIn(query: ErpOtherInQuery): AxiosPromise<ErpOtherInVO[]> {
  return request({ url: '/erp/other-in/orders/list', method: 'get', params: query });
}

export function getOtherIn(id: string | number): AxiosPromise<ErpOtherInVO> {
  return request({ url: `/erp/other-in/orders/${id}`, method: 'get' });
}

export function addOtherIn(data: ErpOtherInForm): AxiosPromise<ErpOtherInVO> {
  return request({ url: '/erp/other-in/orders', method: 'post', data });
}

export function updateOtherIn(data: ErpOtherInForm): AxiosPromise<ErpOtherInVO> {
  return request({ url: `/erp/other-in/orders/${data.id}`, method: 'put', data });
}

export function confirmOtherIn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-in/orders/${id}/confirm`, method: 'post' });
}

export function withdrawOtherIn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-in/orders/${id}/withdraw`, method: 'post' });
}

export function voidOtherIn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-in/orders/${id}/void`, method: 'post' });
}

/** 删除草稿其他入库单（逻辑删除，单号不回收重用） */
export function delOtherIn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/other-in/orders/${id}`, method: 'delete' });
}

/* ------------------------------ 最近成交价（DOC/25 §3.7） ------------------------------ */

/** 该客户最近成交价（无成交兜底全市场），仅供开单参考 */
export function getSaleLastPrice(customerId: string | number, productId: string | number): AxiosPromise<ErpSaleLastPriceVO> {
  return request({ url: '/erp/sales/orders/prices/last', method: 'get', params: { customerId, productId } });
}

/** 该商品的历史成交价（DOC/26 §7），不限客户按业务日期倒序，行上带客户名 */
export function getSalePriceHistory(productId: string | number, limit = 10): AxiosPromise<ErpPriceHistoryVO[]> {
  return request({ url: '/erp/sales/orders/prices/history', method: 'get', params: { productId, limit } });
}

/* ------------------------------ 销售退货（DOC/24） ------------------------------ */

export function listSaleReturn(query: SaleReturnQuery): AxiosPromise<SaleReturnVO[]> {
  return request({ url: '/erp/sale-returns/list', method: 'get', params: query });
}

export function getSaleReturn(id: string | number): AxiosPromise<SaleReturnVO> {
  return request({ url: `/erp/sale-returns/${id}`, method: 'get' });
}

/** 可退原单候选（该客户 CONFIRMED 销售单） */
export function returnableSaleOrders(customerId?: string | number): AxiosPromise<SaleReturnVO[]> {
  return request({ url: '/erp/sale-returns/returnable-orders', method: 'get', params: { customerId } });
}

/** 原单明细（含原行数量/已退量/可退量） */
export function saleReturnOriginalItems(saleId: string | number): AxiosPromise<SaleReturnItemVO[]> {
  return request({ url: `/erp/sale-returns/original-items/${saleId}`, method: 'get' });
}

export function addSaleReturn(data: SaleReturnForm): AxiosPromise<SaleReturnVO> {
  return request({ url: '/erp/sale-returns', method: 'post', data });
}

export function updateSaleReturn(data: SaleReturnForm): AxiosPromise<SaleReturnVO> {
  return request({ url: `/erp/sale-returns/${data.id}`, method: 'put', data });
}

export function confirmSaleReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/sale-returns/${id}/confirm`, method: 'post' });
}

export function withdrawSaleReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/sale-returns/${id}/withdraw`, method: 'post' });
}

export function voidSaleReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/sale-returns/${id}/void`, method: 'post' });
}

/** 删除草稿销售退货单（逻辑删除，单号不回收重用） */
export function delSaleReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/sale-returns/${id}`, method: 'delete' });
}

/* ------------------------------ 采购退货（DOC/24） ------------------------------ */

export function listPurchaseReturn(query: PurchaseReturnQuery): AxiosPromise<PurchaseReturnVO[]> {
  return request({ url: '/erp/purchase-returns/list', method: 'get', params: query });
}

export function getPurchaseReturn(id: string | number): AxiosPromise<PurchaseReturnVO> {
  return request({ url: `/erp/purchase-returns/${id}`, method: 'get' });
}

export function returnablePurchaseOrders(supplierId?: string | number): AxiosPromise<PurchaseReturnVO[]> {
  return request({ url: '/erp/purchase-returns/returnable-orders', method: 'get', params: { supplierId } });
}

export function purchaseReturnOriginalItems(purchaseId: string | number): AxiosPromise<PurchaseReturnItemVO[]> {
  return request({ url: `/erp/purchase-returns/original-items/${purchaseId}`, method: 'get' });
}

export function addPurchaseReturn(data: PurchaseReturnForm): AxiosPromise<PurchaseReturnVO> {
  return request({ url: '/erp/purchase-returns', method: 'post', data });
}

export function updatePurchaseReturn(data: PurchaseReturnForm): AxiosPromise<PurchaseReturnVO> {
  return request({ url: `/erp/purchase-returns/${data.id}`, method: 'put', data });
}

export function confirmPurchaseReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase-returns/${id}/confirm`, method: 'post' });
}

export function withdrawPurchaseReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase-returns/${id}/withdraw`, method: 'post' });
}

export function voidPurchaseReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase-returns/${id}/void`, method: 'post' });
}

/** 删除草稿采购退货单（逻辑删除，单号不回收重用） */
export function delPurchaseReturn(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/purchase-returns/${id}`, method: 'delete' });
}
