package org.erp.stock.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 库存流水视图对象
 */
@Data
public class ErpStockLogVo {

    private Long id;

    /**
     * OPENING/PURCHASE/PROD_IN/PROD_OUT/SALE/OTHER_OUT/OTHER_IN/CHECK/SALE_RETURN/PURCHASE_RETURN/QUICK_IN/VOID
     */
    private String bizType;

    private String bizNo;

    private Long bizId;

    private Long productId;

    private Long warehouseId;

    private BigDecimal qty;

    private BigDecimal unitCost;

    private BigDecimal amount;

    private String partnerType;

    private Long partnerId;

    private String partnerName;

    private BigDecimal balanceAfter;

    private Date bizDate;

    private Integer seq;
}
