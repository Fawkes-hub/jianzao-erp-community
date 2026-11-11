package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 期初往来 VO
 */
@Data
public class FinOpeningVo {

    private Long id;

    private String partyType;

    private Long partyId;

    private String partyName;

    private BigDecimal amount;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private String remark;

    private Date createTime;
}
