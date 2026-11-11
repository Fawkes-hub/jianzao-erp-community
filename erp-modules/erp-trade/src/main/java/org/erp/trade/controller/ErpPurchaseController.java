package org.erp.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
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
import org.erp.trade.domain.bo.ErpPurchaseBo;
import org.erp.trade.domain.vo.ErpPriceHistoryVo;
import org.erp.trade.domain.vo.ErpPurchaseVo;
import org.erp.trade.mapper.ErpPurchasePriceMapper;
import org.erp.trade.service.IErpPurchaseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购入库
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/purchase/orders")
public class ErpPurchaseController extends BaseController {

    private final IErpPurchaseService purchaseService;
    private final ErpPurchasePriceMapper purchasePriceMapper;

    @SaCheckPermission("erp:purchase:list")
    @GetMapping("/list")
    public TableDataInfo<ErpPurchaseVo> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long supplierId,
                                             @RequestParam(required = false) String status,
                                             PageQuery pageQuery) {
        return purchaseService.selectPage(keyword, supplierId, status, pageQuery);
    }

    @SaCheckPermission("erp:purchase:query")
    @GetMapping("/{id}")
    public R<ErpPurchaseVo> getInfo(@PathVariable Long id) {
        return R.ok(purchaseService.selectById(id));
    }

    @SaCheckPermission("erp:purchase:add")
    @PostMapping("/preview-allocation")
    public R<List<ErpPurchaseVo.ItemVo>> previewAllocation(@Valid @RequestBody ErpPurchaseBo bo) {
        return R.ok(purchaseService.previewAllocation(bo));
    }

    @SaCheckPermission("erp:purchase:add")
    @Log(title = "采购入库", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<ErpPurchaseVo> add(@Valid @RequestBody ErpPurchaseBo bo) {
        ConfirmPermission.check("erp:purchase:audit", bo.getConfirm());
        return R.ok(purchaseService.insert(bo));
    }

    @SaCheckPermission("erp:purchase:edit")
    @Log(title = "采购入库", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{id}")
    public R<ErpPurchaseVo> edit(@PathVariable Long id, @Valid @RequestBody ErpPurchaseBo bo) {
        ConfirmPermission.check("erp:purchase:audit", bo.getConfirm());
        return R.ok(purchaseService.update(id, bo));
    }

    @SaCheckPermission("erp:purchase:audit")
    @Log(title = "采购入库确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        purchaseService.confirm(id);
        return R.ok();
    }

    @SaCheckPermission("erp:purchase:withdraw")
    @Log(title = "采购入库撤回", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        purchaseService.withdraw(id);
        return R.ok();
    }

    @SaCheckPermission("erp:purchase:void")
    @Log(title = "采购入库作废", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/void")
    public R<Void> voidBill(@PathVariable Long id) {
        purchaseService.voidBill(id);
        return R.ok();
    }

    /**
     * 该商品的历史采购价列表（DOC/26 §7）：不限供应商，按业务日期倒序，行上带供应商名，供明细下方历史区展示。
     * 路径为字面量段，不会被 /{id} 匹配。
     *
     * @param productId 产品ID
     * @param limit     返回条数，默认 10，限定 1~50
     * @return 历史采购记录
     */
    @SaCheckPermission(value = {"erp:purchase:add", "erp:purchase:edit", "erp:purchase:query"}, mode = SaMode.OR)
    @GetMapping("/prices/history")
    public R<List<ErpPriceHistoryVo>> priceHistory(@RequestParam Long productId,
                                                   @RequestParam(defaultValue = "10") int limit) {
        return R.ok(purchasePriceMapper.selectPriceHistory(productId, Math.min(Math.max(limit, 1), 50)));
    }

    /**
     * 删除草稿采购入库单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 采购单ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase:remove")
    @Log(title = "采购入库", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        purchaseService.deleteDraft(id);
        return R.ok();
    }
}
