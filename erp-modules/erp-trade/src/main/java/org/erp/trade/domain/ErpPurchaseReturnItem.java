package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 采购退货明细 biz_purchase_return_item（DOC/24 §2.3）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_purchase_return_item")
public class ErpPurchaseReturnItem extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long orderId;

    private Long purchaseItemId;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    /**
     * 成本快照 = 原行 cost_price（含运杂费），确认时回填
     */
    private BigDecimal unitCost;

    private BigDecimal returnCost;

    private String remark;

    private Integer seq;

    /**
     * 逻辑删除标志：0存在 1删除；删除退货草稿时明细一并逻辑删除
     */
    @TableLogic
    private String delFlag;
}
