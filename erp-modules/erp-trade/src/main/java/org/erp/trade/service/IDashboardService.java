package org.erp.trade.service;

import org.erp.trade.domain.vo.DashboardPartnerMonthVo;
import org.erp.trade.domain.vo.DashboardStockAlertVo;
import org.erp.trade.domain.vo.DashboardPartnerRankVo;
import org.erp.trade.domain.vo.DashboardSummaryVo;
import org.erp.trade.domain.vo.DashboardTrendVo;

/**
 * 工作台看板服务（DOC/12 §10、DOC/18）
 */
public interface IDashboardService {

    /**
     * 汇总卡片：本月销售/未收未付/库存/生产单数/采购/收款/毛利/草稿待办/最近单据。
     */
    DashboardSummaryVo summary();

    /**
     * 近 12 个月趋势（销售/综合成本/收款/毛利）。
     * 综合成本 = 销售成本 + 其他出库 + 计入成本的其他支出，与综合成本报表同口径。
     */
    DashboardTrendVo trend(Integer months);

    /**
     * 往来单位当月明细：客户看本月销售与收款，供货商看本月采购与付款。
     *
     * @param partyType CUSTOMER=客户，SUPPLIER=供货商
     * @param partyId   往来单位 ID
     * @return 当月汇总数字与单据明细，无数据时各项为 0
     */
    DashboardPartnerMonthVo partnerMonth(String partyType, Long partyId);

    /**
     * 库存上下限预警卡片（DOC/25 §3.3）：低/高计数 + 各 Top5。
     */
    DashboardStockAlertVo stockAlert();

    /**
     * 往来单位本月排行：按本月发生额倒序列出各客户/供货商。
     *
     * @param partyType CUSTOMER=客户，SUPPLIER=供货商
     * @return 本月排行条目，无往来单位时列表为空
     */
    DashboardPartnerRankVo partnerRank(String partyType);
}
