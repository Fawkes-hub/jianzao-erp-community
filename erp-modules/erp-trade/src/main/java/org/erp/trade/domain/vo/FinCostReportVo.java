package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 综合成本/利润 VO（R20，DOC/12 §8 契约）
 */
@Data
public class FinCostReportVo {

    /**
     * 账期 yyyy-MM
     */
    private String month;

    /**
     * 本期销售收入（已确认销售）
     */
    private BigDecimal salesAmount;

    /**
     * 销售成本（已确认销售单的 cost_amount 快照合计）
     */
    private BigDecimal salesCost;

    /**
     * 生产材料成本（参考口径，已包含在销售成本中，不计入综合成本）
     */
    private BigDecimal productionMaterialCost;

    /**
     * 其他出库成本
     */
    private BigDecimal otherOutCost;

    /**
     * 计入成本的其他支出
     */
    private BigDecimal otherExpenseCost;

    /**
     * 本期销售退货金额（CONFIRMED 退货单合计，DOC/24 §6.6）
     */
    private BigDecimal saleReturnAmount;

    /**
     * 本期销售退货成本回冲额
     */
    private BigDecimal saleReturnCostAmount;

    /**
     * 综合成本 = (销售成本 − 退货成本回冲) + 其他出库成本 + 计入成本的其他支出
     */
    private BigDecimal totalCost;

    /**
     * 利润 = 销售收入 - 综合成本
     */
    private BigDecimal grossProfit;

    /**
     * 毛利率 = 利润 / 销售收入 × 100（2 位小数 HALF_UP）；销售收入为 0 时为 null（不可计算）
     */
    private BigDecimal grossMargin;

    /**
     * 库存金额参考值 = Σ st_stock.qty × avg_cost（截至当前，不计入本期损益，DOC/26 §9.5）
     */
    private BigDecimal stockAmount;

    /**
     * 本期成本缺失的销售明细行数（成本金额为 0，DOC/26 §2.4 防线③）；0 表示无缺失
     */
    private Integer costMissingCount;
}
