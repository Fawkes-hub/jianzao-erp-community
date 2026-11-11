package org.erp.stock.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 期初库存 Bo（upsert：product+warehouse 唯一）
 */
@Data
public class StOpeningBo {

    @NotNull(message = "产品不能为空")
    private Long productId;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "期初数量不能为空")
    @PositiveOrZero(message = "期初数量不能为负")
    private BigDecimal qty;

    @NotNull(message = "期初成本不能为空")
    @PositiveOrZero(message = "期初成本不能为负")
    private BigDecimal unitCost;
}
