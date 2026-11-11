package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 其他出库明细 VO
 */
@Data
public class ErpOtherOutItemVo {

    private Long id;

    private Long orderId;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal unitCost;

    private BigDecimal amount;

    private String remark;
}
