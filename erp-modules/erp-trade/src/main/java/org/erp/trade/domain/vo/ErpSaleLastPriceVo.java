package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售最近成交价 VO（DOC/25 §3.7）
 */
@Data
public class ErpSaleLastPriceVo {

    private BigDecimal price;

    private Date bizDate;

    private String saleNo;

    /**
     * CUSTOMER=该客户最近成交 / MARKET=全市场兜底 / NONE=无成交（返回空体时不带该字段）
     */
    private String scope;
}
