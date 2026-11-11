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
import org.erp.trade.domain.bo.ErpPurchaseOrderBo;
import org.erp.trade.domain.vo.ErpPurchaseOrderVo;
import org.erp.trade.service.IErpPurchaseOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 采购订单
 *
 * <p>路径按 DOC/31 §9.1 与执行单层区分：订单层 /erp/purchase-orders，采购入库单仍是 /erp/purchase/orders。
 * 14 个端点与销售订单逐条对称，权限码 erp:purchase-order:*；转单与未交行选择器额外校验采购入库权限 erp:purchase:add。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/purchase-orders")
public class ErpPurchaseOrderController extends BaseController {

    private final IErpPurchaseOrderService purchaseOrderService;

    /**
     * 订单列表：支持关键字、供应商、状态、执行状态、只看未完成、只看已逾期、交期区间。
     *
     * @param keyword      关键字（订单号/供应商单号）
     * @param supplierId   供应商ID
     * @param status       订单状态
     * @param execStatus   执行状态
     * @param unfinished   只看未完成
     * @param overdue      只看已逾期
     * @param dueDateStart 交期起
     * @param dueDateEnd   交期止
     * @param pageQuery    分页参数
     * @return 分页结果
     */
    @SaCheckPermission("erp:purchase-order:list")
    @GetMapping("/list")
    public TableDataInfo<ErpPurchaseOrderVo> list(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long supplierId,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String execStatus,
                                                  @RequestParam(required = false) Boolean unfinished,
                                                  @RequestParam(required = false) Boolean overdue,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dueDateStart,
                                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dueDateEnd,
                                                  PageQuery pageQuery) {
        return purchaseOrderService.selectPage(keyword, supplierId, status, execStatus, unfinished, overdue,
            dueDateStart, dueDateEnd, pageQuery);
    }

    /**
     * 订单详情（含订单行）。
     *
     * @param id 订单头ID
     * @return 订单详情
     */
    @SaCheckPermission("erp:purchase-order:query")
    @GetMapping("/{id}")
    public R<ErpPurchaseOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(purchaseOrderService.selectById(id));
    }

    /**
     * 新建订单（草稿）。
     *
     * @param bo 订单入参
     * @return 新建后的订单
     */
    @SaCheckPermission("erp:purchase-order:add")
    @Log(title = "采购订单", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<ErpPurchaseOrderVo> add(@Valid @RequestBody ErpPurchaseOrderBo bo) {
        return R.ok(purchaseOrderService.insert(bo));
    }

    /**
     * 编辑订单：草稿整单保存；已确认只允许改约定字段并写变更留痕。
     *
     * @param id 订单头ID
     * @param bo 订单入参
     * @return 编辑后的订单
     */
    @SaCheckPermission("erp:purchase-order:edit")
    @Log(title = "采购订单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{id}")
    public R<ErpPurchaseOrderVo> edit(@PathVariable Long id, @Valid @RequestBody ErpPurchaseOrderBo bo) {
        return R.ok(purchaseOrderService.update(id, bo));
    }

    /**
     * 删除草稿订单（逻辑删除，单号不回收）。
     *
     * @param id 订单头ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase-order:remove")
    @Log(title = "采购订单", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        purchaseOrderService.deleteDraft(id);
        return R.ok();
    }

    /**
     * 确认订单：DRAFT → CONFIRMED。
     *
     * @param id 订单头ID
     * @return 确认后的订单
     */
    @SaCheckPermission("erp:purchase-order:audit")
    @Log(title = "采购订单确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/confirm")
    public R<ErpPurchaseOrderVo> confirm(@PathVariable Long id) {
        return R.ok(purchaseOrderService.confirm(id));
    }

    /**
     * 撤回订单：CONFIRMED → DRAFT，要求无执行量且无未处理入库单。
     *
     * @param id 订单头ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase-order:withdraw")
    @Log(title = "采购订单撤回", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        purchaseOrderService.withdraw(id);
        return R.ok();
    }

    /**
     * 作废订单：CONFIRMED → VOID。
     *
     * @param id 订单头ID
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase-order:void")
    @Log(title = "采购订单作废", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/void")
    public R<Void> voidBill(@PathVariable Long id) {
        purchaseOrderService.voidBill(id);
        return R.ok();
    }

    /**
     * 整单关闭剩余：原因必填。
     *
     * @param id 订单头ID
     * @param bo 关闭原因
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase-order:edit")
    @Log(title = "采购订单关闭剩余", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id, @RequestBody(required = false) ErpPurchaseOrderBo.CloseBo bo) {
        purchaseOrderService.closeOrder(id, bo == null ? null : bo.getReason());
        return R.ok();
    }

    /**
     * 单行关闭剩余：原因必填。
     *
     * @param id     订单头ID
     * @param itemId 订单行ID
     * @param bo     关闭原因
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase-order:edit")
    @Log(title = "采购订单关闭剩余", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/items/{itemId}/close")
    public R<Void> closeItem(@PathVariable Long id, @PathVariable Long itemId,
                             @RequestBody(required = false) ErpPurchaseOrderBo.CloseBo bo) {
        purchaseOrderService.closeItem(id, itemId, bo == null ? null : bo.getReason());
        return R.ok();
    }

    /**
     * 单行重开：整单重开由前端逐行调用（D27，不设 /{id}/reopen）。
     *
     * @param id     订单头ID
     * @param itemId 订单行ID
     * @param bo     重开原因，可空
     * @return 操作结果
     */
    @SaCheckPermission("erp:purchase-order:edit")
    @Log(title = "采购订单重开", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/items/{itemId}/reopen")
    public R<Void> reopenItem(@PathVariable Long id, @PathVariable Long itemId,
                              @RequestBody(required = false) ErpPurchaseOrderBo.CloseBo bo) {
        purchaseOrderService.reopenItem(id, itemId, bo == null ? null : bo.getReason());
        return R.ok();
    }

    /**
     * 转采购入库单：生成 DRAFT 入库单并返回其ID；额外要求 erp:purchase:add。
     *
     * @param id 订单头ID
     * @param bo 转单入参（不传 items 默认带全部未交行）
     * @return 生成的采购入库单ID
     */
    @SaCheckPermission(value = {"erp:purchase-order:add", "erp:purchase:add"}, mode = SaMode.AND)
    @Log(title = "采购订单转入库单", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/{id}/receipts")
    public R<Long> createReceipt(@PathVariable Long id,
                                 @RequestBody(required = false) ErpPurchaseOrderBo.ReceiptBo bo) {
        return R.ok(purchaseOrderService.createReceipt(id, bo));
    }

    /**
     * 执行记录：入库单 + 退货单，按业务日期倒序。
     *
     * @param id 订单头ID
     * @return 执行记录
     */
    @SaCheckPermission("erp:purchase-order:query")
    @GetMapping("/{id}/executions")
    public R<List<ErpPurchaseOrderVo.ExecRecordVo>> executions(@PathVariable Long id) {
        return R.ok(purchaseOrderService.selectExecutions(id));
    }

    /**
     * 未交行选择器：只能按订单 ID 取，不接受按供应商跨单拉行（避免跨单混选）。
     *
     * @param orderId 订单头ID，必填
     * @return 未交订单行
     */
    @SaCheckPermission("erp:purchase:add")
    @GetMapping("/open-items")
    public R<List<ErpPurchaseOrderVo.ItemVo>> openItems(@RequestParam Long orderId) {
        return R.ok(purchaseOrderService.selectOpenItems(orderId));
    }
}
