package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收款核销 VO
 */
@Data
public class FinReceiptAllocVo {

    private Long id;

    private Long receiptId;

    private Long arApId;

    private BigDecimal amount;
}
