package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 应收应付台账 VO
 */
@Data
public class FinArApVo {

    private Long id;

    private String partyType;

    private Long partyId;

    private String partyName;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private Date bizDate;

    private BigDecimal amount;

    private BigDecimal settledAmount;

    private BigDecimal unsettledAmount;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;
}
