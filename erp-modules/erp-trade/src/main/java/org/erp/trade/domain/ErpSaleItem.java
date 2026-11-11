package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 销售明细 biz_sale_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_sale_item")
public class ErpSaleItem extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long orderId;

    /**
     * 来源销售订单行ID，NULL=直开出库单明细
     */
    private Long sourceOrderItemId;

    private Long productId;

    /**
     * 现录现卖商品快照 JSON（草稿阶段 product_id=0，确认时据此建品并入库，DOC/26 §8.3）
     */
    private String newProductSnapshot;

    private BigDecimal qty;

    /**
     * 销售单价（一单一价）
     */
    private BigDecimal price;

    /**
     * 发票类型快照（继承表头）
     */
    private String taxType;

    /**
     * 税率%快照（继承表头）
     */
    private BigDecimal taxRate;

    private BigDecimal amount;

    /**
     * 成本单价快照（含分摊运杂费）
     */
    private BigDecimal unitCost;

    private BigDecimal costAmount;

    /**
     * 行毛利
     */
    private BigDecimal grossProfit;

    private String remark;

    private Integer seq;

    @TableLogic
    private String delFlag;
}
