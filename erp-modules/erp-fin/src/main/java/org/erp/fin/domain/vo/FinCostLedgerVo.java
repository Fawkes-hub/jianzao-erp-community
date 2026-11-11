package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成本台账 VO
 */
@Data
public class FinCostLedgerVo {

    private Long id;

    private String costType;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private Date bizDate;

    private BigDecimal amount;

    private String status;

    private String remark;
}
