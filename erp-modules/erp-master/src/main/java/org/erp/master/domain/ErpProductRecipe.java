package org.erp.master.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 单层配比 md_product_recipe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_product_recipe")
public class ErpProductRecipe extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long productId;

    private Long materialId;

    private BigDecimal qtyPerUnit;

    @TableLogic
    private String delFlag;
}
