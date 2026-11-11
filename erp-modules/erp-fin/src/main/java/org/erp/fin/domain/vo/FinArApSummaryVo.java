package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 应收/应付汇总 VO（S8）
 *
 * 总额 + 已结 + 未结 + 按客户/供应商分组明细；
 * AR 时 openingAmount = 期初应收（fin_ar_ap source_type=OPENING 合计）。
 */
@Data
public class FinArApSummaryVo {

    private String partyType;

    private BigDecimal totalAmount;

    private BigDecimal settledAmount;

    private BigDecimal unsettledAmount;

    private BigDecimal openingAmount;

    private List<PartySummary> parties = new ArrayList<>();

    @Data
    public static class PartySummary {

        private Long partyId;

        private String partyName;

        private BigDecimal amount;

        private BigDecimal settled;

        private BigDecimal unsettled;

        private BigDecimal opening;
    }
}
