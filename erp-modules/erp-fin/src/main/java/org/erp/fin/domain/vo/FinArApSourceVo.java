package org.erp.fin.domain.vo;

import lombok.Data;

/**
 * 台账行的来源信息（收付款单核销明细回显用）
 *
 * 台账表 fin_ar_ap 只存 source_type + source_id，业务单号需按类型回查业务单表；
 * 该 VO 供收付款单详情把「外部台账 id」换成用户能认出的单号。
 */
@Data
public class FinArApSourceVo {

    /**
     * 台账行 id（fin_ar_ap.id）
     */
    private Long arApId;

    /**
     * 来源类型：SALE/PURCHASE/SALE_RETURN/PURCHASE_RETURN/OPENING/SUBCONTRACT
     */
    private String sourceType;

    /**
     * 来源单号；OPENING 等无单号的来源为 null
     */
    private String sourceNo;
}
