package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 其他出库明细 biz_other_out_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_other_out_item")
public class ErpOtherOutItem extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long orderId;

    private Long productId;

    private BigDecimal qty;

    /**
     * 成本单价快照（确认时按库存 avg_cost）
     */
    private BigDecimal unitCost;

    private BigDecimal amount;

    private String remark;

    /**
     * 逻辑删除标志：0存在 1删除；删除其他出库草稿时明细一并逻辑删除
     */
    @TableLogic
    private String delFlag;
}
