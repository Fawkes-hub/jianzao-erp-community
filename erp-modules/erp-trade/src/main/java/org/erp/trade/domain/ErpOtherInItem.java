package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 其他入库明细 biz_other_in_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_other_in_item")
public class ErpOtherInItem extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long orderId;

    private Long productId;

    private BigDecimal qty;

    /**
     * 录入入库单价（确认时原样入账）
     */
    private BigDecimal unitCost;

    private BigDecimal amount;

    private String remark;

    /**
     * 逻辑删除标志：0存在 1删除；删除其他入库草稿时明细一并逻辑删除
     */
    @TableLogic
    private String delFlag;
}
