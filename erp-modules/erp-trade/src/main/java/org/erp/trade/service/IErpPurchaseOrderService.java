package org.erp.trade.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.trade.domain.bo.ErpPurchaseOrderBo;
import org.erp.trade.domain.vo.ErpPurchaseOrderVo;

import java.util.Date;
import java.util.List;

/**
 * 采购订单服务接口（DOC/31 §9.2）
 *
 * <p>与销售订单服务逐条同构：CRUD + 确认/撤回/作废 + 关闭剩余（整单/单行）+ 行级重开 +
 * 转采购入库单 + 执行记录 + 未交行选择器。状态、数量、加锁与错误码全部沿用同一口径。
 */
public interface IErpPurchaseOrderService {

    /**
     * 分页查询采购订单。
     *
     * @param keyword      关键字（订单号/供应商单号），可空
     * @param supplierId   供应商ID，可空
     * @param status       订单状态 DRAFT/CONFIRMED/VOID，可空
     * @param execStatus   执行状态 NOT_STARTED/PARTIAL/FULL/CLOSED，可空
     * @param unfinished   true=只看未完成（CONFIRMED 且 NOT_STARTED/PARTIAL）
     * @param overdue      true=只看已逾期（今天 > 交期 + 允许延迟天数，且未交量 > 0）
     * @param dueDateStart 交期起（含），可空
     * @param dueDateEnd   交期止（含），可空
     * @param pageQuery    分页参数
     * @return 分页结果，行上带执行进度与未交量合计
     */
    TableDataInfo<ErpPurchaseOrderVo> selectPage(String keyword, Long supplierId, String status, String execStatus,
                                                 Boolean unfinished, Boolean overdue,
                                                 Date dueDateStart, Date dueDateEnd, PageQuery pageQuery);

    /**
     * 查询订单详情（含全部订单行）。
     *
     * @param id 订单头ID
     * @return 订单详情
     */
    ErpPurchaseOrderVo selectById(Long id);

    /**
     * 新建采购订单（草稿），保存后立即重算交期与执行进度。
     *
     * @param bo 订单入参
     * @return 新建后的订单详情
     */
    ErpPurchaseOrderVo insert(ErpPurchaseOrderBo bo);

    /**
     * 编辑采购订单：草稿可整单重保存；已确认只允许改数量/单价/交期/承诺交期/备注与约定表头字段，并写变更留痕。
     *
     * @param id 订单头ID
     * @param bo 订单入参
     * @return 编辑后的订单详情
     */
    ErpPurchaseOrderVo update(Long id, ErpPurchaseOrderBo bo);

    /**
     * 删除草稿订单（头与行逻辑删除，单号不回收）；重复删除幂等成功。
     *
     * @param id 订单头ID
     */
    void deleteDraft(Long id);

    /**
     * 确认订单：DRAFT → CONFIRMED，状态 CAS + 订单头锁。
     *
     * @param id 订单头ID
     * @return 确认后的订单详情
     */
    ErpPurchaseOrderVo confirm(Long id);

    /**
     * 撤回订单：CONFIRMED → DRAFT，要求无执行量且无未处理入库单。
     *
     * @param id 订单头ID
     */
    void withdraw(Long id);

    /**
     * 作废订单：CONFIRMED → VOID，前置条件同撤回。
     *
     * @param id 订单头ID
     */
    void voidBill(Long id);

    /**
     * 整单关闭剩余：把所有还有未交量的行置为已关闭，原因必填。
     *
     * @param id     订单头ID
     * @param reason 关闭原因
     */
    void closeOrder(Long id, String reason);

    /**
     * 单行关闭剩余：原因必填。
     *
     * @param id     订单头ID
     * @param itemId 订单行ID
     * @param reason 关闭原因
     */
    void closeItem(Long id, Long itemId, String reason);

    /**
     * 单行重开：closed_flag=1 → 0，closed_qty 归 0；整单重开由前端逐行调用（D27）。
     *
     * @param id     订单头ID
     * @param itemId 订单行ID
     * @param reason 重开原因，可空
     */
    void reopenItem(Long id, Long itemId, String reason);

    /**
     * 转采购入库单：按选中订单行与数量生成一张 DRAFT 采购入库单，不改订单执行量。
     *
     * @param id  订单头ID
     * @param bo  转单入参；items 为空时默认带全部未交行、数量＝未交量
     * @return 生成的采购入库单ID
     */
    Long createReceipt(Long id, ErpPurchaseOrderBo.ReceiptBo bo);

    /**
     * 查询订单关联的执行记录（入库单 + 退货单），按业务日期倒序。
     *
     * @param id 订单头ID
     * @return 执行记录
     */
    List<ErpPurchaseOrderVo.ExecRecordVo> selectExecutions(Long id);

    /**
     * 未交行选择器：只返回指定订单 CONFIRMED 且未交量 > 0 的订单行。
     *
     * @param orderId 订单头ID（必填，不接受按供应商跨单拉行）
     * @return 未交订单行
     */
    List<ErpPurchaseOrderVo.ItemVo> selectOpenItems(Long orderId);
}
