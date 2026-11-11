import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type {
  FinArApQuery,
  FinArApSummaryVO,
  FinArApVO,
  FinOpeningForm,
  FinOpeningVO,
  FinOtherFlowForm,
  FinOtherFlowQuery,
  FinOtherFlowVO,
  FinPaymentForm,
  FinPaymentQuery,
  FinPaymentVO,
  FinReceiptForm,
  FinReceiptQuery,
  FinReceiptVO
} from './types';
import type { ErpImportResult } from './master';

export type { ErpImportResult } from './master';

/* ------------------------------ 应收应付台账 ------------------------------ */

export function listArAp(query: FinArApQuery): AxiosPromise<FinArApVO[]> {
  return request({ url: '/erp/finance/ar-ap/list', method: 'get', params: query });
}

/** 汇总：总额 + 按客户/供应商分组 */
export function arApSummary(partyType: string): AxiosPromise<FinArApSummaryVO> {
  return request({ url: '/erp/finance/ar-ap/summary', method: 'get', params: { partyType } });
}

/** 单客户/供应商台账明细 */
export function arApDetail(partyType: string, partyId: string | number): AxiosPromise<FinArApVO[]> {
  return request({ url: '/erp/finance/ar-ap/detail', method: 'get', params: { partyType, partyId } });
}

/* ------------------------------ 往来期初 ------------------------------ */

export function listFinOpening(partyType?: string): AxiosPromise<FinOpeningVO[]> {
  return request({ url: '/erp/finance/openings/list', method: 'get', params: { partyType } });
}

/** 期初往来导出（按往来类型筛选，后端不分页） */
export function exportFinOpening(query: { partyType?: string }): AxiosPromise<Blob> {
  return request({ url: '/erp/finance/openings/export', method: 'post', params: query, responseType: 'blob' });
}

/** 期初往来导入（multipart，updateSupport 由调用方填入 FormData） */
export function importFinOpening(data: FormData): AxiosPromise<ErpImportResult> {
  return request({ url: '/erp/finance/openings/importData', method: 'post', data });
}

/** 下载期初往来导入模板 */
export function finOpeningImportTemplate(): AxiosPromise<Blob> {
  return request({ url: '/erp/finance/openings/importTemplate', method: 'post', responseType: 'blob' });
}

/** 期初往来录入（upsert，party_type + party_id 唯一） */
export function saveFinOpening(data: FinOpeningForm): AxiosPromise<FinOpeningVO> {
  return request({ url: '/erp/finance/openings', method: 'post', data });
}

/** 期初确认：落 fin_ar_ap OPENING */
export function confirmFinOpening(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/openings/${id}/confirm`, method: 'post' });
}

/** 期初反确认：冲销台账（已核销拒 40904） */
export function unconfirmFinOpening(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/openings/${id}/unconfirm`, method: 'post' });
}

/* ------------------------------ 收款单 ------------------------------ */

export function listReceipt(query: FinReceiptQuery): AxiosPromise<FinReceiptVO[]> {
  return request({ url: '/erp/finance/receipts/list', method: 'get', params: query });
}

export function getReceipt(id: string | number): AxiosPromise<FinReceiptVO> {
  return request({ url: `/erp/finance/receipts/${id}`, method: 'get' });
}

export function addReceipt(data: FinReceiptForm): AxiosPromise<FinReceiptVO> {
  return request({ url: '/erp/finance/receipts', method: 'post', data });
}

export function updateReceipt(data: FinReceiptForm): AxiosPromise<FinReceiptVO> {
  return request({ url: `/erp/finance/receipts/${data.id}`, method: 'put', data });
}

export function confirmReceipt(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/receipts/${id}/confirm`, method: 'post' });
}

export function withdrawReceipt(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/receipts/${id}/withdraw`, method: 'post' });
}

export function voidReceipt(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/receipts/${id}/void`, method: 'post' });
}

/** 删除草稿收款单（逻辑删除，仅 DRAFT 可删） */
export function delReceipt(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/receipts/${id}`, method: 'delete' });
}

/* ------------------------------ 付款单 ------------------------------ */

export function listPayment(query: FinPaymentQuery): AxiosPromise<FinPaymentVO[]> {
  return request({ url: '/erp/finance/payments/list', method: 'get', params: query });
}

export function getPayment(id: string | number): AxiosPromise<FinPaymentVO> {
  return request({ url: `/erp/finance/payments/${id}`, method: 'get' });
}

export function addPayment(data: FinPaymentForm): AxiosPromise<FinPaymentVO> {
  return request({ url: '/erp/finance/payments', method: 'post', data });
}

export function updatePayment(data: FinPaymentForm): AxiosPromise<FinPaymentVO> {
  return request({ url: `/erp/finance/payments/${data.id}`, method: 'put', data });
}

export function confirmPayment(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/payments/${id}/confirm`, method: 'post' });
}

export function withdrawPayment(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/payments/${id}/withdraw`, method: 'post' });
}

export function voidPayment(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/payments/${id}/void`, method: 'post' });
}

/** 删除草稿付款单（逻辑删除，仅 DRAFT 可删） */
export function delPayment(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/payments/${id}`, method: 'delete' });
}

/* ------------------------------ 其他收支 ------------------------------ */

export function listOtherFlow(query: FinOtherFlowQuery): AxiosPromise<FinOtherFlowVO[]> {
  return request({ url: '/erp/finance/other-flows/list', method: 'get', params: query });
}

export function getOtherFlow(id: string | number): AxiosPromise<FinOtherFlowVO> {
  return request({ url: `/erp/finance/other-flows/${id}`, method: 'get' });
}

export function addOtherFlow(data: FinOtherFlowForm): AxiosPromise<FinOtherFlowVO> {
  return request({ url: '/erp/finance/other-flows', method: 'post', data });
}

export function updateOtherFlow(data: FinOtherFlowForm): AxiosPromise<FinOtherFlowVO> {
  return request({ url: `/erp/finance/other-flows/${data.id}`, method: 'put', data });
}

export function confirmOtherFlow(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/other-flows/${id}/confirm`, method: 'post' });
}

export function withdrawOtherFlow(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/other-flows/${id}/withdraw`, method: 'post' });
}

export function voidOtherFlow(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/other-flows/${id}/void`, method: 'post' });
}

/** 删除草稿其他收支单（逻辑删除，仅 DRAFT 可删） */
export function delOtherFlow(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/other-flows/${id}`, method: 'delete' });
}

/* ------------------------------ 综合成本 ------------------------------ */


