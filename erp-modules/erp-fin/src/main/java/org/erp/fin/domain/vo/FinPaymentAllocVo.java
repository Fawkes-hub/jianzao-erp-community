package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 付款核销 VO
 */
@Data
public class FinPaymentAllocVo {

    private Long id;

    private Long paymentId;

    private Long arApId;

    private BigDecimal amount;
}
