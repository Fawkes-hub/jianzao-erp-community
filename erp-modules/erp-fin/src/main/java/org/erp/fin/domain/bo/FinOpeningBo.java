package org.erp.fin.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 期初往来 Bo（upsert：party_type+party_id 唯一）
 */
@Data
public class FinOpeningBo {

    @NotBlank(message = "往来类型不能为空")
    @Pattern(regexp = "AR|AP", message = "往来类型必须是 AR 或 AP")
    private String partyType;

    @NotNull(message = "往来单位不能为空")
    private Long partyId;

    @NotNull(message = "期初金额不能为空")
    @PositiveOrZero(message = "期初金额不能为负")
    private BigDecimal amount;

    private String remark;
}
