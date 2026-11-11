import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type {
  ErpCustomerForm,
  ErpCustomerVO,
  ErpPartyQuery,
  ErpProductCategoryForm,
  ErpProductCategoryVO,
  ErpProductForm,
  ErpProductQuery,
  ErpProductVO,
  ErpRecipeItem,
  ErpRecipeItemVO,
  ErpSupplierForm,
  ErpSupplierVO,
  ErpWarehouseForm,
  ErpWarehouseVO
} from './types';

/** 导入错误行（row 为 Excel 行号，含表头行） */
export interface ErpImportError {
  row: number;
  message: string;
}

/** 导入结果（与后端 org.erp.common.excel.ImportResult 对应） */
export interface ErpImportResult {
  total: number;
  success: number;
  skipped: number;
  failed: number;
  errors: ErpImportError[];
}

/* ------------------------------ 产品 ------------------------------ */

export function listProduct(query: ErpProductQuery): AxiosPromise<ErpProductVO[]> {
  return request({ url: '/erp/master/products/list', method: 'get', params: query });
}

export function getProduct(id: string | number): AxiosPromise<ErpProductVO> {
  return request({ url: `/erp/master/products/${id}`, method: 'get' });
}

/** 产品下拉选项，远程搜索；categoryId 非空时按商品分类过滤，type 非空时按成品/原料过滤（DOC/26 §4.4） */
export function productOptions(keyword?: string, categoryId?: string | number, type?: string): AxiosPromise<ErpProductVO[]> {
  return request({ url: '/erp/master/products/options', method: 'get', params: { keyword, categoryId, type } });
}

export function addProduct(data: ErpProductForm): AxiosPromise<void> {
  return request({ url: '/erp/master/products', method: 'post', data });
}

export function updateProduct(data: ErpProductForm): AxiosPromise<void> {
  return request({ url: `/erp/master/products/${data.id}`, method: 'put', data });
}

export function delProduct(id: string | number | (string | number)[]): AxiosPromise<void> {
  return request({ url: `/erp/master/products/${id}`, method: 'delete' });
}

/** 导出产品（按当前筛选条件，后端不分页） */
export function exportProduct(query: ErpProductQuery): AxiosPromise<Blob> {
  return request({ url: '/erp/master/products/export', method: 'post', params: query, responseType: 'blob' });
}

/** 导入产品（multipart，updateSupport 由调用方填入 FormData） */
export function importProduct(data: FormData): AxiosPromise<ErpImportResult> {
  return request({ url: '/erp/master/products/importData', method: 'post', data });
}

/** 下载产品导入模板 */
export function productImportTemplate(): AxiosPromise<Blob> {
  return request({ url: '/erp/master/products/importTemplate', method: 'post', responseType: 'blob' });
}

export function getRecipe(id: string | number): AxiosPromise<ErpRecipeItemVO[]> {
  return request({ url: `/erp/master/products/${id}/recipe`, method: 'get' });
}

export function saveRecipe(id: string | number, items: ErpRecipeItem[]): AxiosPromise<void> {
  return request({ url: `/erp/master/products/${id}/recipe`, method: 'put', data: items });
}

/* ------------------------------ 商品分类 ------------------------------ */

/** 分类下拉/分组用列表（含停用分类，按排序升序） */
export function productCategoryOptions(keyword?: string): AxiosPromise<ErpProductCategoryVO[]> {
  return request({ url: '/erp/master/product-categories/options', method: 'get', params: { keyword } });
}

export function addProductCategory(data: ErpProductCategoryForm): AxiosPromise<void> {
  return request({ url: '/erp/master/product-categories', method: 'post', data });
}

export function updateProductCategory(data: ErpProductCategoryForm): AxiosPromise<void> {
  return request({ url: `/erp/master/product-categories/${data.id}`, method: 'put', data });
}

export function delProductCategory(id: string | number | (string | number)[]): AxiosPromise<void> {
  return request({ url: `/erp/master/product-categories/${id}`, method: 'delete' });
}

/* ------------------------------ 供应商 ------------------------------ */

export function listSupplier(query: ErpPartyQuery): AxiosPromise<ErpSupplierVO[]> {
  return request({ url: '/erp/master/suppliers/list', method: 'get', params: query });
}

export function getSupplier(id: string | number): AxiosPromise<ErpSupplierVO> {
  return request({ url: `/erp/master/suppliers/${id}`, method: 'get' });
}

export function supplierOptions(keyword?: string): AxiosPromise<ErpSupplierVO[]> {
  return request({ url: '/erp/master/suppliers/options', method: 'get', params: { keyword } });
}

export function addSupplier(data: ErpSupplierForm): AxiosPromise<void> {
  return request({ url: '/erp/master/suppliers', method: 'post', data });
}

export function updateSupplier(data: ErpSupplierForm): AxiosPromise<void> {
  return request({ url: `/erp/master/suppliers/${data.id}`, method: 'put', data });
}

export function delSupplier(id: string | number | (string | number)[]): AxiosPromise<void> {
  return request({ url: `/erp/master/suppliers/${id}`, method: 'delete' });
}

/** 导出供应商（按当前筛选条件，后端不分页） */
export function exportSupplier(query: ErpPartyQuery): AxiosPromise<Blob> {
  return request({ url: '/erp/master/suppliers/export', method: 'post', params: query, responseType: 'blob' });
}

/** 导入供应商（multipart，updateSupport 由调用方填入 FormData） */
export function importSupplier(data: FormData): AxiosPromise<ErpImportResult> {
  return request({ url: '/erp/master/suppliers/importData', method: 'post', data });
}

/** 下载供应商导入模板 */
export function supplierImportTemplate(): AxiosPromise<Blob> {
  return request({ url: '/erp/master/suppliers/importTemplate', method: 'post', responseType: 'blob' });
}

/* ------------------------------ 客户 ------------------------------ */

export function listCustomer(query: ErpPartyQuery): AxiosPromise<ErpCustomerVO[]> {
  return request({ url: '/erp/master/customers/list', method: 'get', params: query });
}

export function getCustomer(id: string | number): AxiosPromise<ErpCustomerVO> {
  return request({ url: `/erp/master/customers/${id}`, method: 'get' });
}

export function customerOptions(keyword?: string): AxiosPromise<ErpCustomerVO[]> {
  return request({ url: '/erp/master/customers/options', method: 'get', params: { keyword } });
}

export function addCustomer(data: ErpCustomerForm): AxiosPromise<void> {
  return request({ url: '/erp/master/customers', method: 'post', data });
}

export function updateCustomer(data: ErpCustomerForm): AxiosPromise<void> {
  return request({ url: `/erp/master/customers/${data.id}`, method: 'put', data });
}

export function delCustomer(id: string | number | (string | number)[]): AxiosPromise<void> {
  return request({ url: `/erp/master/customers/${id}`, method: 'delete' });
}

/** 导出客户（按当前筛选条件，后端不分页） */
export function exportCustomer(query: ErpPartyQuery): AxiosPromise<Blob> {
  return request({ url: '/erp/master/customers/export', method: 'post', params: query, responseType: 'blob' });
}

/** 导入客户（multipart，updateSupport 由调用方填入 FormData） */
export function importCustomer(data: FormData): AxiosPromise<ErpImportResult> {
  return request({ url: '/erp/master/customers/importData', method: 'post', data });
}

/** 下载客户导入模板 */
export function customerImportTemplate(): AxiosPromise<Blob> {
  return request({ url: '/erp/master/customers/importTemplate', method: 'post', responseType: 'blob' });
}

/* ------------------------------ 仓库 ------------------------------ */

export function listWarehouse(query: ErpPartyQuery): AxiosPromise<ErpWarehouseVO[]> {
  return request({ url: '/erp/master/warehouses/list', method: 'get', params: query });
}

export function warehouseOptions(keyword?: string): AxiosPromise<ErpWarehouseVO[]> {
  return request({ url: '/erp/master/warehouses/options', method: 'get', params: { keyword } });
}

export function addWarehouse(data: ErpWarehouseForm): AxiosPromise<void> {
  return request({ url: '/erp/master/warehouses', method: 'post', data });
}

export function updateWarehouse(data: ErpWarehouseForm): AxiosPromise<void> {
  return request({ url: `/erp/master/warehouses/${data.id}`, method: 'put', data });
}

export function delWarehouse(id: string | number | (string | number)[]): AxiosPromise<void> {
  return request({ url: `/erp/master/warehouses/${id}`, method: 'delete' });
}

/** 导出仓库（按当前筛选条件，后端不分页） */
export function exportWarehouse(query: ErpPartyQuery): AxiosPromise<Blob> {
  return request({ url: '/erp/master/warehouses/export', method: 'post', params: query, responseType: 'blob' });
}

/** 导入仓库（multipart，updateSupport 由调用方填入 FormData） */
export function importWarehouse(data: FormData): AxiosPromise<ErpImportResult> {
  return request({ url: '/erp/master/warehouses/importData', method: 'post', data });
}

/** 下载仓库导入模板 */
export function warehouseImportTemplate(): AxiosPromise<Blob> {
  return request({ url: '/erp/master/warehouses/importTemplate', method: 'post', responseType: 'blob' });
}
