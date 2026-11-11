package org.erp.common.enums;

import java.util.Map;

/**
 * 单据状态中文文案（后端单一来源）。
 *
 * 单据状态与应收应付台账状态共用 VOID 码但文案不同，故按状态族分表维护：
 * - 单据（BILL）：DRAFT/CONFIRMED/VOID → 草稿/已确认/已作废；
 * - 应收应付台账（AR_AP）：OPEN/PARTIAL/SETTLED/VOID → 未核销/部分核销/已核销/已冲销；
 * - 订单执行进度（EXEC）：NOT_STARTED/PARTIAL/FULL/CLOSED → 未执行/部分执行/已完成/已关闭。
 *
 * 各 Service 的 toVo(...) 用本类回填 VO 的 statusText，前端直接展示，
 * 不再由每个 Service 各自散落维护文案表。
 */
public final class StatusText {

    /**
     * 单据状态文案：DRAFT=草稿，CONFIRMED=已确认，VOID=已作废。
     */
    private static final Map<String, String> BILL = Map.of(
        "DRAFT", "草稿",
        "CONFIRMED", "已确认",
        "VOID", "已作废");

    /**
     * 应收应付台账状态文案：OPEN=未核销，PARTIAL=部分核销，SETTLED=已核销，VOID=已冲销。
     */
    private static final Map<String, String> AR_AP = Map.of(
        "OPEN", "未核销",
        "PARTIAL", "部分核销",
        "SETTLED", "已核销",
        "VOID", "已冲销");

    /**
     * 订单执行进度文案：NOT_STARTED=未执行，PARTIAL=部分执行，FULL=已完成，CLOSED=已关闭。
     */
    private static final Map<String, String> EXEC = Map.of(
        "NOT_STARTED", "未执行",
        "PARTIAL", "部分执行",
        "FULL", "已完成",
        "CLOSED", "已关闭");

    private StatusText() {
    }

    /**
     * 取单据状态文案。
     *
     * @param status 状态码；为空返回 null，未登记的状态码原样返回
     * @return 状态中文文案
     */
    public static String bill(String status) {
        return text(BILL, status);
    }

    /**
     * 取应收应付台账状态文案。
     *
     * @param status 状态码；为空返回 null，未登记的状态码原样返回
     * @return 状态中文文案
     */
    public static String arAp(String status) {
        return text(AR_AP, status);
    }

    /**
     * 按文案表查状态文案。
     *
     * @param map    文案表
     * @param status 状态码；为空返回 null，未登记的状态码原样返回
     * @return 状态中文文案
     */
    private static String text(Map<String, String> map, String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return map.getOrDefault(status, status);
    }

    /**
     * 取订单执行进度文案（DOC/31 §5.1、§6.1.1）。
     *
     * @param execStatus 执行状态码；为空返回 null，未登记的状态码原样返回
     * @return 状态中文文案
     */
    public static String exec(String execStatus) {
        return text(EXEC, execStatus);
    }
}
