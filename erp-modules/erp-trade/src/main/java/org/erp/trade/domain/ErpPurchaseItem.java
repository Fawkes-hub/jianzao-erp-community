package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 采购明细 biz_purchase_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_purchase_item")
public class ErpPurchaseItem extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long orderId;

    /**
     * 来源采购订单行ID，NULL=直开入库单明细
     */
    private Long sourceOrderItemId;

    private Long productId;

    private BigDecimal qty;

    /**
     * 采购单价
     */
    private BigDecimal price;

    /**
     * 发票类型快照 NO_TAX/VAT_INVOICE/GENERAL_INVOICE
     */
    private String taxType;

    private BigDecimal taxRate;

    /**
     * 货品金额 = qty × price
     */
    private BigDecimal amount;

    /**
     * 本行分摊运杂费
     */
    private BigDecimal freightAmount;

    /**
     * 每件运杂费
     */
    private BigDecimal freightPerUnit;

    /**
     * 成本单价（含分摊运杂费；增值税按不含税）
     */
    private BigDecimal costPrice;

    private String remark;

    private Integer seq;

    @TableLogic
    private String delFlag;
}
