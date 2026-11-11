package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 结算账户 VO（DOC/25 §3.4）
 */
@Data
public class FinAccountVo {

    private Long id;

    private String code;

    private String name;

    /**
     * CASH/BANK/WECHAT/ALIPAY/OTHER
     */
    private String type;

    private BigDecimal openingBalance;

    private Integer status;

    private String remark;

    /**
     * 实时余额 = 期初 + Σ收 − Σ付（仅详情/列表按需计算）
     */
    private BigDecimal balance;

    private Date createTime;
}
