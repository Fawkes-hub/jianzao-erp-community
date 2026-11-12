package org.erp.trade.service;

import org.erp.trade.domain.vo.FinCostReportVo;

/**
 * 综合成本/利润报表（R20）
 *
 * 综合成本 = 销售成本（已确认销售单 cost_amount）+ 其他出库成本 + 计入成本的其他支出；
 * 生产材料成本作为参考返回，不计入综合成本（避免与销售成本重复）；
 * 销售收入 = 本月已确认销售金额。
 * 实现位于 erp-trade（需同时读 biz_sale 与 fin_cost_ledger，避免 fin→trade 成环）。
 */
public interface IFinCostReportService {

    /**
     * 月度利润表 + 库存金额参考值（期初/入库形成的库存资产，不计入本期损益）
     *
     * @param month 账期 yyyy-MM；空 = 当前月
     */
    FinCostReportVo report(String month);
}
