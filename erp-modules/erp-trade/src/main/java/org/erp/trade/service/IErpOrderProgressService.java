package org.erp.trade.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单执行进度服务（采购/销售共用，DOC/31 §6.1.3、§6.7、§6.8.1）。
 *
 * <p>加锁顺序一经确定，全链路照抄：单据头（执行单/退货单） → 订单头 → 订单行（id ASC）
 * → fin_ar_ap 既有行 → st_stock（product_id ASC） → fin_ar_ap 新行。
 * 本服务只负责「订单头 → 订单行」两级：调用方必须先锁自身单据头，再进本服务，最后才动库存与台账。
 * 所有方法都必须在调用方事务内执行（FOR UPDATE 依赖事务生效），本服务不单独开事务。
 */
public interface IErpOrderProgressService {

    /**
     * 采购订单
     */
    String TYPE_PURCHASE = "PURCHASE";

    /**
     * 销售订单
     */
    String TYPE_SALE = "SALE";

    /**
     * 锁订单头，再按 id 升序锁全部订单行。
     * 撤回/作废/退货等「只改数量不改上限」的链路在入口调用，保证与确认路径的加锁顺序一致。
     *
     * @param orderType 订单类型 PURCHASE/SALE
     * @param orderId   订单头ID；为空（直开执行单）直接返回
     */
    void lockOrder(String orderType, Long orderId);

    /**
     * 执行单确认前加锁并校验：禁空行、行归属、本批数量上限与超收容差。
     *
     * @param orderType 订单类型 PURCHASE/SALE
     * @param orderId   订单头ID；为空（直开执行单）直接返回
     * @param lines     执行单全部行（含未挂订单行的空行），挂订单时出现空行即 40902
     */
    void lockAndValidate(String orderType, Long orderId, List<ExecLine> lines);

    /**
     * 重算订单执行进度：executed_qty = Σ已确认执行量 − Σ已确认退货量，closed_qty 只做下界保护，
     * 覆盖写订单头 due_date 与 exec_status。幂等，可重跑。
     *
     * <p>调用约定：本事务内第一条数据库读必须是锁定读（selectByIdForUpdate）。MySQL 默认 REPEATABLE READ，
     * 一致性快照在首次普通 SELECT 时创建；先普通读再锁订单头，聚合会沿用旧快照、漏掉并发事务刚提交的执行单。
     *
     * @param orderType     订单类型 PURCHASE/SALE
     * @param orderId       订单头ID；为空直接返回
     * @param excludeBillId 需要排除的执行单ID（撤回/作废在本单状态落库前调用时传本单ID），可空
     */
    void recalc(String orderType, Long orderId, Long excludeBillId);

    /**
     * 执行单行：订单行ID + 本批数量。
     *
     * @param orderItemId 订单行ID，为空表示该行未挂订单行
     * @param qty         本批执行数量
     */
    record ExecLine(Long orderItemId, BigDecimal qty) {
    }
}
