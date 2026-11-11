import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type {
  ErpId,
  ErpStockAlertVO,
  ErpStockLogVO,
  ErpStockQuery,
  ErpStockVO,
  StOpeningForm,
  StOpeningVO
} from './types';
import type { ErpImportResult } from './master';

export type { ErpImportResult } from './master';

/** 库存余额（成品/原料） */
export function listStockBalances(query: ErpStockQuery): AxiosPromise<ErpStockVO[]> {
  return request({ url: '/erp/stock/balances', method: 'get', params: query });
}

/** 库存上下限预警明细（DOC/25 §3.2，不分页） */
export function listStockAlerts(query: { type?: string; productId?: ErpId }): AxiosPromise<ErpStockAlertVO[]> {
  return request({ url: '/erp/stock/alerts', method: 'get', params: query });
}

/** 按产品查库存流水 */
export function listStockLogs(productId: string | number): AxiosPromise<ErpStockLogVO[]> {
  return request({ url: '/erp/stock/logs', method: 'get', params: { productId } });
}

/** 查询单个产品的库存余额 */
export function getStockBalance(productId: string | number): AxiosPromise<ErpStockVO> {
  return request({ url: `/erp/stock/balances/${productId}`, method: 'get' });
}

/* ------------------------------ 库存期初 ------------------------------ */

export function listStOpening(warehouseId?: string | number): AxiosPromise<StOpeningVO[]> {
  return request({ url: '/erp/stock/opening/list', method: 'get', params: { warehouseId } });
}

/** 期初库存导出（按仓库筛选，后端不分页） */
export function exportStOpening(query: { warehouseId?: ErpId }): AxiosPromise<Blob> {
  return request({ url: '/erp/stock/opening/export', method: 'post', params: query, responseType: 'blob' });
}

/** 期初库存导入（multipart，updateSupport 由调用方填入 FormData） */
export function importStOpening(data: FormData): AxiosPromise<ErpImportResult> {
  return request({ url: '/erp/stock/opening/importData', method: 'post', data });
}

/** 下载期初库存导入模板 */
export function stOpeningImportTemplate(): AxiosPromise<Blob> {
  return request({ url: '/erp/stock/opening/importTemplate', method: 'post', responseType: 'blob' });
}

/** 期初录入（upsert，product + warehouse 唯一） */
export function saveStOpening(data: StOpeningForm): AxiosPromise<StOpeningVO> {
  return request({ url: '/erp/stock/opening', method: 'post', data });
}

/** 期初确认锁定（写 OPENING 流水） */
export function confirmStOpening(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/stock/opening/${id}/confirm`, method: 'post' });
}

/** 期初反确认（reverse 回退） */
export function unconfirmStOpening(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/stock/opening/${id}/unconfirm`, method: 'post' });
}

/** 删除草稿库存期初（逻辑删除，单号不回收重用） */
export function delStOpening(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/stock/opening/${id}`, method: 'delete' });
}
