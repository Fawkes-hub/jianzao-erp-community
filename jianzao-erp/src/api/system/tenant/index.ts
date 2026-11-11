import request from '@/utils/request';
import { TenantForm, TenantQuery, TenantSwitchResult, TenantVO } from './types';
import { AxiosPromise } from 'axios';

// 查询租户列表
export function listTenant(query: TenantQuery): AxiosPromise<TenantVO[]> {
  return request({
    url: '/system/tenant/list',
    method: 'get',
    params: query
  });
}

// 查询租户详细
export function getTenant(id: string | number): AxiosPromise<TenantVO> {
  return request({
    url: '/system/tenant/' + id,
    method: 'get'
  });
}

// 新增租户
export function addTenant(data: TenantForm) {
  return request({
    url: '/system/tenant',
    method: 'post',
    headers: {
      isEncrypt: true,
      repeatSubmit: false
    },
    data: data
  });
}

// 修改租户
export function updateTenant(data: TenantForm) {
  return request({
    url: '/system/tenant',
    method: 'put',
    data: data
  });
}

// 租户状态修改
export function changeTenantStatus(id: string | number, tenantId: string | number, status: string) {
  const data = {
    id,
    tenantId,
    status
  };
  return request({
    url: '/system/tenant/changeStatus',
    method: 'put',
    data: data
  });
}

// 删除租户
export function delTenant(id: string | number | Array<string | number>) {
  return request({
    url: '/system/tenant/' + id,
    method: 'delete'
  });
}

/**
 * 平台超管进入企业：只切换数据过滤（dynamicTenant），权限仍是平台超管（DOC/44 §4.3）
 * 用现有 GET 接口，B 分支新增 POST 后仍保留旧 GET 一个版本（DOC/45 §5.2），两侧都能调用
 * @param tenantId 目标企业
 */
export function dynamicTenant(tenantId: string | number) {
  return request({
    url: '/system/tenant/dynamic/' + tenantId,
    method: 'get'
  });
}

// 清除动态租户（平台超管退出企业）
export function dynamicClear() {
  return request({
    url: '/system/tenant/dynamic/clear',
    method: 'get'
  });
}

/**
 * 当前生效企业信息：用于校准顶栏展示（平台超管进入企业后刷新页面时对齐动态租户）
 * B 分支才会新增该接口；调用方用 silentHttpError 静默 404，并回退本地缓存
 */
export function getCurrentTenant(): AxiosPromise<TenantSwitchResult> {
  return request({
    url: '/system/tenant/current',
    method: 'get',
    headers: {
      silentHttpError: true
    }
  });
}

// 同步租户套餐
export function syncTenantPackage(tenantId: string | number, packageId: string | number) {
  const data = {
    tenantId,
    packageId
  };
  return request({
    url: '/system/tenant/syncTenantPackage',
    method: 'get',
    params: data
  });
}

// 同步租户字典
export function syncTenantDict() {
  return request({
    url: '/system/tenant/syncTenantDict',
    method: 'get'
  });
}

// 同步租户字典
export function syncTenantConfig() {
  return request({
    url: '/system/tenant/syncTenantConfig',
    method: 'get'
  });
}
