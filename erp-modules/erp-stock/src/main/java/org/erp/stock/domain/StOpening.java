package org.erp.stock.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 期初库存 st_opening
 *
 * product+warehouse 唯一；DRAFT/CONFIRMED；确认后锁定（40902）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("st_opening")
public class StOpening extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long productId;

    private Long warehouseId;

    private BigDecimal qty;

    private BigDecimal unitCost;

    private String status;

    /**
     * 逻辑删除标志：0存在 1删除；草稿期初删除走逻辑删除。
     * 唯一键 uk_st_opening 含 product_id+warehouse_id 不含 del_flag，删除后重建走「复活原行」而非插入新行。
     */
    @TableLogic
    private String delFlag;
}
