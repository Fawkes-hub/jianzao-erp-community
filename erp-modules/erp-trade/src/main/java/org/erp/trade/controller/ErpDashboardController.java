package org.erp.trade.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.erp.trade.domain.vo.DashboardPartnerMonthVo;
import org.erp.trade.domain.vo.DashboardStockAlertVo;
import org.erp.trade.domain.vo.DashboardPartnerRankVo;
import org.erp.trade.domain.vo.DashboardSummaryVo;
import org.erp.trade.domain.vo.DashboardTrendVo;
import org.erp.trade.service.IDashboardService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台看板（DOC/12 §10、DOC/18）
 *
 * 登录即可访问：三角色均需工作台数据，不设按钮级权限。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/dashboard")
public class ErpDashboardController extends BaseController {

    private final IDashboardService dashboardService;

    /**
     * 汇总卡片
     */
    @SaCheckLogin
    @GetMapping("/summary")
    public R<DashboardSummaryVo> summary() {
        return R.ok(dashboardService.summary());
    }

    /**
     * 近 N 月趋势（默认 6，最大 12）
     */
    @SaCheckLogin
    @GetMapping("/trend")
    public R<DashboardTrendVo> trend(@RequestParam(required = false) Integer months) {
        return R.ok(dashboardService.trend(months));
    }

    /**
     * 往来单位当月明细：客户看本月销售与收款，供货商看本月采购与付款。
     *
     * @param partyType CUSTOMER=客户，SUPPLIER=供货商
     * @param partyId   往来单位 ID，缺省时由服务层给出「请选择往来单位」提示
     */
    @SaCheckLogin
    @GetMapping("/partner-month")
    public R<DashboardPartnerMonthVo> partnerMonth(@RequestParam(required = false) String partyType,
                                                    @RequestParam(required = false) Long partyId) {
        return R.ok(dashboardService.partnerMonth(partyType, partyId));
    }

    /**
     * 库存上下限预警卡片（DOC/25 §3.3）。
     */
    @SaCheckLogin
    @GetMapping("/stock-alert")
    public R<DashboardStockAlertVo> stockAlert() {
        return R.ok(dashboardService.stockAlert());
    }

    /**
     * 往来单位本月排行：按本月发生额倒序列出各客户/供货商。
     *
     * @param partyType CUSTOMER=客户，SUPPLIER=供货商
     */
    @SaCheckLogin
    @GetMapping("/partner-rank")
    public R<DashboardPartnerRankVo> partnerRank(@RequestParam(required = false) String partyType) {
        return R.ok(dashboardService.partnerRank(partyType));
    }
}
