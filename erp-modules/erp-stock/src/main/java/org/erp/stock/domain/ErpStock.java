package org.erp.stock.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 库存快照 st_stock
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("st_stock")
public class ErpStock extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long productId;

    private Long warehouseId;

    private BigDecimal qty;

    /**
     * 移动加权平均成本
     */
    private BigDecimal avgCost;
}
