package org.erp.master.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 配比行业务对象
 */
@Data
public class ErpRecipeItemBo {

    @NotNull(message = "原料不能为空")
    private Long materialId;

    @NotNull(message = "用量不能为空")
    private BigDecimal qtyPerUnit;
}
