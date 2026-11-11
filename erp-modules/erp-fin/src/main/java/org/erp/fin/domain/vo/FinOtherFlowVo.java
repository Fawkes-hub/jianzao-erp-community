package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 其他收支 VO
 */
@Data
public class FinOtherFlowVo {

    private Long id;

    private String no;

    private String flowType;

    private String category;

    private BigDecimal amount;

    private Date bizDate;

    private String account;

    /**
     * 结算账户ID（存量单为空）
     */
    private Long accountId;

    private Integer affectCost;

    private String remark;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private Date createTime;
}
