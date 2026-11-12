package org.erp.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.erp.trade.domain.vo.FinCostReportVo;
import org.erp.trade.service.IFinCostReportService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 综合成本/利润报表
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/finance/cost-report")
public class FinCostReportController extends BaseController {

    private final IFinCostReportService reportService;

    @SaCheckPermission("erp:finance:cost-report")
    @GetMapping
    public R<FinCostReportVo> report(@RequestParam(required = false) String month) {
        return R.ok(reportService.report(month));
    }
}
