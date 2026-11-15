import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import type {
  DashboardPartnerMonthVO,
  DashboardPartnerRankVO,
  DashboardStockAlertVO,
  DashboardSummaryVO,
  DashboardTrendVO,
  ErpPartnerType
} from './types';

/** 汇总卡片：服务端聚合，前端不做业务计算 */
export function dashboardSummary(): AxiosPromise<DashboardSummaryVO> {
  return request({ url: '/erp/dashboard/summary', method: 'get' });
}

/** 经营趋势，months 缺省 6，范围 1~12 */
export function dashboardTrend(months?: number): AxiosPromise<DashboardTrendVO> {
  return request({ url: '/erp/dashboard/trend', method: 'get', params: { months } });
}

/** 库存上下限预警 */
export function dashboardStockAlert(): AxiosPromise<DashboardStockAlertVO> {
  return request({ url: '/erp/dashboard/stock-alert', method: 'get' });
}

/** 往来单位当月明细：客户看本月销售与收款，供货商看本月采购与付款 */
export function dashboardPartnerMonth(partyType: ErpPartnerType, partyId: string | number): AxiosPromise<DashboardPartnerMonthVO> {
  return request({ url: '/erp/dashboard/partner-month', method: 'get', params: { partyType, partyId } });
}

/** 往来单位本月排行：按本月发生额倒序 */
export function dashboardPartnerRank(partyType: ErpPartnerType): AxiosPromise<DashboardPartnerRankVO> {
  return request({ url: '/erp/dashboard/partner-rank', method: 'get', params: { partyType } });
}
