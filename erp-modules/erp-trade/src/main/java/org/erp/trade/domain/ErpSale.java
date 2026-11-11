package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售出库单 biz_sale
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_sale")
public class ErpSale extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String no;

    /**
     * 来源销售订单ID，NULL=直开出库单
     */
    private Long sourceOrderId;

    /**
     * 来源销售订单号快照
     */
    private String sourceOrderNo;

    /**
     * 客户联系人快照（转单时从订单带入）
     */
    private String contactName;

    /**
     * 客户联系电话快照
     */
    private String contactPhone;

    /**
     * 送货地址快照（转单时从订单带入，后续打印取快照不回查订单）
     */
    private String deliveryAddress;

    private Long customerId;

    private Date bizDate;

    private String status;

    /**
     * 发票类型（整单统一） NO_TAX/VAT_INVOICE/GENERAL_INVOICE
     */
    private String taxType;

    /**
     * 税率%（整单统一）
     */
    private BigDecimal taxRate;

    /**
     * 货品金额（不含运杂费）
     */
    private BigDecimal goodsAmount;

    /**
     * 运杂费总额
     */
    private BigDecimal freightAmount;

    /**
     * 运杂费分摊方式（固定 BY_QTY）
     */
    private String freightAllocMethod;

    private BigDecimal totalAmount;

    /**
     * 成本金额快照
     */
    private BigDecimal costAmount;

    /**
     * 毛利 = totalAmount - costAmount
     */
    private BigDecimal grossProfit;

    /**
     * 毛利率%
     */
    private BigDecimal grossMargin;

    private String remark;

    private Long confirmedBy;

    private Date confirmedAt;

    private Long withdrawnBy;

    private Date withdrawnAt;

    private Long voidedBy;

    private Date voidedAt;

    @TableLogic
    private String delFlag;
}
