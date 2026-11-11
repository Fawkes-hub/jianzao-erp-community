package org.erp.fin.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 其他收支 Bo
 */
@Data
public class FinOtherFlowBo {

    @NotBlank(message = "收支类型不能为空")
    @Pattern(regexp = "INCOME|EXPENSE", message = "收支类型必须是 INCOME 或 EXPENSE")
    private String flowType;

    private String category;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "业务日期不能为空")
    private Date bizDate;

    private String account;

    /**
     * 结算账户ID（新增必填；存量草稿编辑放行空值，DOC/25 §3.6）
     */
    private Long accountId;

    /**
     * 是否计入综合成本（仅支出有效，默认 0）
     */
    private Integer affectCost;

    private String remark;

    /**
     * true = 保存并确认
     */
    private Boolean confirm;
}
