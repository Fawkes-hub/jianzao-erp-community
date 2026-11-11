package org.erp.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.erp.common.security.ConfirmPermission;
import org.erp.trade.domain.bo.ErpOtherInBo;
import org.erp.trade.domain.vo.ErpOtherInVo;
import org.erp.trade.service.IErpOtherInService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 其他入库
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/other-in/orders")
public class ErpOtherInController extends BaseController {

    private final IErpOtherInService otherInService;

    @SaCheckPermission("erp:other-in:list")
    @GetMapping("/list")
    public TableDataInfo<ErpOtherInVo> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String reason,
                                            @RequestParam(required = false) String status,
                                            PageQuery pageQuery) {
        return otherInService.selectPage(keyword, reason, status, pageQuery);
    }

    @SaCheckPermission("erp:other-in:query")
    @GetMapping("/{id}")
    public R<ErpOtherInVo> getInfo(@PathVariable Long id) {
        return R.ok(otherInService.selectById(id));
    }

    @SaCheckPermission("erp:other-in:add")
    @Log(title = "其他入库", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<ErpOtherInVo> add(@Valid @RequestBody ErpOtherInBo bo) {
        ConfirmPermission.check("erp:other-in:audit", bo.getConfirm());
        return R.ok(otherInService.insert(bo));
    }

    @SaCheckPermission("erp:other-in:edit")
    @Log(title = "其他入库", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{id}")
    public R<ErpOtherInVo> edit(@PathVariable Long id, @Valid @RequestBody ErpOtherInBo bo) {
        ConfirmPermission.check("erp:other-in:audit", bo.getConfirm());
        return R.ok(otherInService.update(id, bo));
    }

    @SaCheckPermission("erp:other-in:audit")
    @Log(title = "其他入库确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        otherInService.confirm(id);
        return R.ok();
    }

    @SaCheckPermission("erp:other-in:withdraw")
    @Log(title = "其他入库撤回", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        otherInService.withdraw(id);
        return R.ok();
    }

    @SaCheckPermission("erp:other-in:void")
    @Log(title = "其他入库作废", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/void")
    public R<Void> voidBill(@PathVariable Long id) {
        otherInService.voidBill(id);
        return R.ok();
    }

    /**
     * 删除草稿其他入库单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 其他入库单ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:other-in:remove")
    @Log(title = "其他入库", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        otherInService.deleteDraft(id);
        return R.ok();
    }
}
