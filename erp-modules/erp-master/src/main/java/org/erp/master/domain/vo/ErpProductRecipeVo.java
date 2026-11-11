package org.erp.master.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.erp.master.domain.ErpProductRecipe;

import java.math.BigDecimal;

/**
 * 配比行视图对象
 */
@Data
@AutoMapper(target = ErpProductRecipe.class)
public class ErpProductRecipeVo {

    private Long id;

    private Long productId;

    private Long materialId;

    private BigDecimal qtyPerUnit;
}
