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
 * 采购入库单 biz_purchase
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_purchase")
public class ErpPurchase extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String no;

    /**
     * 来源采购订单ID，NULL=直开入库单
     */
    private Long sourceOrderId;

    /**
     * 来源采购订单号快照
     */
    private String sourceOrderNo;

    /**
     * 供应商联系人快照（转单时从订单带入，详情与打印取快照不回查订单）
     */
    private String contactName;

    /**
     * 供应商联系电话快照
     */
    private String contactPhone;

    private Long supplierId;

    private Date bizDate;

    /**
     * DRAFT/CONFIRMED/VOID
     */
    private String status;

    /**
     * 发票类型（整单统一） NO_TAX/VAT_INVOICE/GENERAL_INVOICE
     */
    private String taxType;

    /**
     * 税率%（整单统一）
     */
    private BigDecimal taxRate;

    private BigDecimal goodsAmount;

    /**
     * 运杂费总额
     */
    private BigDecimal freightAmount;

    /**
     * BY_AMOUNT/BY_QTY/MANUAL
     */
    private String freightAllocMethod;

    private BigDecimal totalAmount;

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
