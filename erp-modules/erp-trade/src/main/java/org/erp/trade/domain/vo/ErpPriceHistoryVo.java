package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 历史价格 VO（DOC/26 §7）：销售、采购通用，取已确认单据的成交记录；按商品维度查询，行上带对方名称
 */
@Data
public class ErpPriceHistoryVo {

    /** 业务日期 */
    private Date bizDate;

    /** 单据号 */
    private String orderNo;

    /** 成交单价 */
    private BigDecimal price;

    /** 数量 */
    private BigDecimal qty;

    /** 行金额 */
    private BigDecimal amount;

    /** 交易对方名称：销售为客户名，采购为供应商名 */
    private String partyName;
}
