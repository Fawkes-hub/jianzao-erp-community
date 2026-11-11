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
import org.erp.trade.domain.bo.ErpOtherOutBo;
import org.erp.trade.domain.vo.ErpOtherOutVo;
import org.erp.trade.service.IErpOtherOutService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 其他出库
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/other-out/orders")
public class ErpOtherOutController extends BaseController {

    private final IErpOtherOutService otherOutService;

    @SaCheckPermission("erp:other-out:list")
    @GetMapping("/list")
    public TableDataInfo<ErpOtherOutVo> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String costCategory,
                                             @RequestParam(required = false) String status,
                                             PageQuery pageQuery) {
        return otherOutService.selectPage(keyword, costCategory, status, pageQuery);
    }

    @SaCheckPermission("erp:other-out:query")
    @GetMapping("/{id}")
    public R<ErpOtherOutVo> getInfo(@PathVariable Long id) {
        return R.ok(otherOutService.selectById(id));
    }

    @SaCheckPermission("erp:other-out:add")
    @Log(title = "其他出库", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<ErpOtherOutVo> add(@Valid @RequestBody ErpOtherOutBo bo) {
        ConfirmPermission.check("erp:other-out:audit", bo.getConfirm());
        return R.ok(otherOutService.insert(bo));
    }

    @SaCheckPermission("erp:other-out:edit")
    @Log(title = "其他出库", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{id}")
    public R<ErpOtherOutVo> edit(@PathVariable Long id, @Valid @RequestBody ErpOtherOutBo bo) {
        ConfirmPermission.check("erp:other-out:audit", bo.getConfirm());
        return R.ok(otherOutService.update(id, bo));
    }

    @SaCheckPermission("erp:other-out:audit")
    @Log(title = "其他出库确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        otherOutService.confirm(id);
        return R.ok();
    }

    @SaCheckPermission("erp:other-out:withdraw")
    @Log(title = "其他出库撤回", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        otherOutService.withdraw(id);
        return R.ok();
    }

    @SaCheckPermission("erp:other-out:void")
    @Log(title = "其他出库作废", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/void")
    public R<Void> voidBill(@PathVariable Long id) {
        otherOutService.voidBill(id);
        return R.ok();
    }

    /**
     * 删除草稿其他出库单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 其他出库单ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:other-out:remove")
    @Log(title = "其他出库", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        otherOutService.deleteDraft(id);
        return R.ok();
    }
}
