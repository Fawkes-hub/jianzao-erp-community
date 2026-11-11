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
import org.erp.trade.domain.bo.ErpSaleBo;
import org.erp.trade.domain.vo.ErpPriceHistoryVo;
import org.erp.trade.domain.vo.ErpSaleLastPriceVo;
import org.erp.trade.domain.vo.ErpSaleVo;
import org.erp.trade.mapper.ErpSalePriceMapper;
import org.erp.trade.service.IErpSaleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 销售出库
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/sales/orders")
public class ErpSaleController extends BaseController {

    private final IErpSaleService saleService;
    private final ErpSalePriceMapper salePriceMapper;

    @SaCheckPermission("erp:sale:list")
    @GetMapping("/list")
    public TableDataInfo<ErpSaleVo> list(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long customerId,
                                         @RequestParam(required = false) String status,
                                         PageQuery pageQuery) {
        return saleService.selectPage(keyword, customerId, status, pageQuery);
    }

    @SaCheckPermission("erp:sale:query")
    @GetMapping("/{id}")
    public R<ErpSaleVo> getInfo(@PathVariable Long id) {
        return R.ok(saleService.selectById(id));
    }

    @SaCheckPermission("erp:sale:add")
    @Log(title = "销售出库", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<ErpSaleVo> add(@Valid @RequestBody ErpSaleBo bo) {
        ConfirmPermission.check("erp:sale:audit", bo.getConfirm());
        return R.ok(saleService.insert(bo));
    }

    @SaCheckPermission("erp:sale:edit")
    @Log(title = "销售出库", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{id}")
    public R<ErpSaleVo> edit(@PathVariable Long id, @Valid @RequestBody ErpSaleBo bo) {
        ConfirmPermission.check("erp:sale:audit", bo.getConfirm());
        return R.ok(saleService.update(id, bo));
    }

    @SaCheckPermission("erp:sale:audit")
    @Log(title = "销售出库确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/confirm")
    public R<ErpSaleVo> confirm(@PathVariable Long id) {
        return R.ok(saleService.confirm(id));
    }

    @SaCheckPermission("erp:sale:withdraw")
    @Log(title = "销售出库撤回", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        saleService.withdraw(id);
        return R.ok();
    }

    @SaCheckPermission("erp:sale:void")
    @Log(title = "销售出库作废", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/void")
    public R<Void> voidBill(@PathVariable Long id) {
        saleService.voidBill(id);
        return R.ok();
    }

    /**
     * 最近成交价参考（DOC/25 §3.7）：先查该客户，无成交兜底全市场；只读参考，不校验区间。
     * 新增/编辑开单都可能调（SaCheckPermission orMode）。
     */
    @SaCheckPermission(value = {"erp:sale:add", "erp:sale:edit"}, mode = SaMode.OR)
    @GetMapping("/prices/last")
    public R<ErpSaleLastPriceVo> lastPrice(@RequestParam Long customerId,
                                           @RequestParam Long productId) {
        ErpSaleLastPriceVo vo = salePriceMapper.selectLastPrice(customerId, productId, "CUSTOMER");
        if (vo == null) {
            vo = salePriceMapper.selectLastPrice(customerId, productId, "MARKET");
        }
        return R.ok(vo);
    }

    /**
     * 该商品的历史成交价列表（DOC/26 §7）：不限客户，按业务日期倒序，行上带客户名，供明细下方历史区展示。
     * 路径与 prices/last 同级，均为字面量段，不会被 /{id} 匹配。
     *
     * @param productId 产品ID
     * @param limit     返回条数，默认 10，限定 1~50
     * @return 历史成交记录
     */
    @SaCheckPermission(value = {"erp:sale:add", "erp:sale:edit", "erp:sale:query"}, mode = SaMode.OR)
    @GetMapping("/prices/history")
    public R<List<ErpPriceHistoryVo>> priceHistory(@RequestParam Long productId,
                                                   @RequestParam(defaultValue = "10") int limit) {
        return R.ok(salePriceMapper.selectPriceHistory(productId, Math.min(Math.max(limit, 1), 50)));
    }

    /**
     * 删除草稿销售出库单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 销售单ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:sale:remove")
    @Log(title = "销售出库", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        saleService.deleteDraft(id);
        return R.ok();
    }
}
