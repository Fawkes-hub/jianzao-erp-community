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
import org.erp.trade.domain.bo.ErpPurchaseReturnBo;
import org.erp.trade.domain.vo.ErpPurchaseReturnVo;
import org.erp.trade.service.IErpPurchaseReturnService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购退货（DOC/24 §3.2）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/purchase-returns")
public class ErpPurchaseReturnController extends BaseController {

    private final IErpPurchaseReturnService returnService;

    @SaCheckPermission("erp:purchase-return:list")
    @GetMapping("/list")
    public TableDataInfo<ErpPurchaseReturnVo> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long supplierId,
                                                   @RequestParam(required = false) String status,
                                                   PageQuery pageQuery) {
        return returnService.selectPage(keyword, supplierId, status, pageQuery);
    }

    @SaCheckPermission("erp:purchase-return:query")
    @GetMapping("/{id}")
    public R<ErpPurchaseReturnVo> getInfo(@PathVariable Long id) {
        return R.ok(returnService.selectById(id));
    }

    @SaCheckPermission("erp:purchase-return:list")
    @GetMapping("/returnable-orders")
    public R<List<ErpPurchaseReturnVo>> returnableOrders(@RequestParam(required = false) Long supplierId) {
        return R.ok(returnService.returnableOrders(supplierId));
    }

    @SaCheckPermission("erp:purchase-return:list")
    @GetMapping("/original-items/{purchaseId}")
    public R<List<ErpPurchaseReturnVo.ItemVo>> originalItems(@PathVariable Long purchaseId) {
        return R.ok(returnService.originalItems(purchaseId));
    }

    @SaCheckPermission("erp:purchase-return:add")
    @Log(title = "采购退货", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<ErpPurchaseReturnVo> add(@Valid @RequestBody ErpPurchaseReturnBo bo) {
        ConfirmPermission.check("erp:purchase-return:audit", bo.getConfirm());
        return R.ok(returnService.insert(bo));
    }

    @SaCheckPermission("erp:purchase-return:edit")
    @Log(title = "采购退货", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{id}")
    public R<ErpPurchaseReturnVo> edit(@PathVariable Long id, @Valid @RequestBody ErpPurchaseReturnBo bo) {
        ConfirmPermission.check("erp:purchase-return:audit", bo.getConfirm());
        return R.ok(returnService.update(id, bo));
    }

    @SaCheckPermission("erp:purchase-return:audit")
    @Log(title = "采购退货确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        returnService.confirm(id);
        return R.ok();
    }

    @SaCheckPermission("erp:purchase-return:withdraw")
    @Log(title = "采购退货撤回", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        returnService.withdraw(id);
        return R.ok();
    }

    @SaCheckPermission("erp:purchase-return:void")
    @Log(title = "采购退货作废", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/void")
    public R<Void> voidBill(@PathVariable Long id) {
        returnService.voidBill(id);
        return R.ok();
    }

    /**
     * 删除草稿采购退货单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 采购退货单ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase-return:remove")
    @Log(title = "采购退货", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        returnService.deleteDraft(id);
        return R.ok();
    }
}
