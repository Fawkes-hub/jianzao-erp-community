package org.erp.fin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.erp.fin.domain.vo.FinArApSummaryVo;
import org.erp.fin.domain.vo.FinArApVo;
import org.erp.fin.service.IFinArApService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 应收应付台账
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/finance/ar-ap")
public class FinArApController extends BaseController {

    private final IFinArApService arApService;

    /**
     * 台账分页
     */
    @SaCheckPermission("erp:finance:list")
    @GetMapping("/list")
    public TableDataInfo<FinArApVo> list(@RequestParam(required = false) String partyType,
                                         @RequestParam(required = false) Long partyId,
                                         @RequestParam(required = false) String status,
                                         PageQuery pageQuery) {
        return arApService.selectPage(partyType, partyId, status, pageQuery);
    }

    /**
     * S8：总应收/总应付 + 按客户/供应商分组
     */
    @SaCheckPermission("erp:finance:list")
    @GetMapping("/summary")
    public R<FinArApSummaryVo> summary(@RequestParam String partyType) {
        return R.ok(arApService.summary(partyType));
    }

    /**
     * S8：单客户/供应商明细
     */
    @SaCheckPermission("erp:finance:list")
    @GetMapping("/detail")
    public R<List<FinArApVo>> detail(@RequestParam String partyType, @RequestParam Long partyId) {
        return R.ok(arApService.listByParty(partyType, partyId));
    }
}
