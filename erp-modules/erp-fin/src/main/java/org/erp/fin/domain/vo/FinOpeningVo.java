package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * 期初截止日（含当日），决定期初在哪个账期可见
     */
    private LocalDate openingDate;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private String remark;

    private Date createTime;

    /**
     * 保存时是否覆盖了该往来单位已有的草稿期初（仅 save 返回时填充，用于前端提示）
     */
    private Boolean overwritten;
}
