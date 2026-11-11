import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type { FinAccountFlowVO, FinAccountForm, FinAccountQuery, FinAccountVO } from './types';

/* ------------------------------ 结算账户（DOC/25） ------------------------------ */

export function listAccount(query: FinAccountQuery): AxiosPromise<FinAccountVO[]> {
  return request({ url: '/erp/finance/accounts/list', method: 'get', params: query });
}

export function getAccount(id: string | number): AxiosPromise<FinAccountVO> {
  return request({ url: `/erp/finance/accounts/${id}`, method: 'get' });
}

export function addAccount(data: FinAccountForm): AxiosPromise<void> {
  return request({ url: '/erp/finance/accounts', method: 'post', data });
}

export function updateAccount(data: FinAccountForm): AxiosPromise<void> {
  return request({ url: `/erp/finance/accounts/${data.id}`, method: 'put', data });
}

export function deleteAccount(id: string | number): AxiosPromise<void> {
  return request({ url: `/erp/finance/accounts/${id}`, method: 'delete' });
}

/** 启用账户下拉（收付款/其他收支页面复用） */
export function accountOptions(): AxiosPromise<FinAccountVO[]> {
  return request({ url: '/erp/finance/accounts/options', method: 'get' });
}

export function listAccountFlows(id: string | number, query: PageQuery): AxiosPromise<FinAccountFlowVO[]> {
  return request({ url: `/erp/finance/accounts/${id}/flows`, method: 'get', params: query });
}
