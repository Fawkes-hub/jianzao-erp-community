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
 * 采购订单 biz_purchase_order（DOC/31 §5.5.1）
 *
 * <p>与销售订单 biz_sale_order 同构，差异只有四处：供应商字段、供应商单号、跟单人命名、无送货地址。
 * 状态家族两套：status（DRAFT/CONFIRMED/VOID，人工动作）与 exec_status（NOT_STARTED/PARTIAL/FULL/CLOSED，
 * 由 IErpOrderProgressService#recalc 覆盖写），due_date 与 exec_status 都是派生冗余列。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_purchase_order")
public class ErpPurchaseOrder extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    /**
     * 订单号 CGD+yyyyMMdd+4位流水
     */
    private String no;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 下单日期（不等于交期）
     */
    private Date bizDate;

    /**
     * 派生冗余：存在未交量的行里最早的 due_date；全部交完/关闭时取全部行的最早交期（不置空），recalc 覆盖写
     */
    private Date dueDate;

    /**
     * DRAFT/CONFIRMED/VOID
     */
    private String status;

    /**
     * NOT_STARTED/PARTIAL/FULL/CLOSED，recalc 覆盖写
     */
    private String execStatus;

    /**
     * 供应商单号（对方单号）
     */
    private String supplierOrderNo;

    /**
     * 跟单人 sys_user.id
     */
    private Long buyerId;

    /**
     * 跟单人姓名快照
     */
    private String buyerName;

    /**
     * 供应商联系人（转单时快照到执行单头）
     */
    private String contactName;

    /**
     * 供应商联系电话（转单时快照到执行单头）
     */
    private String contactPhone;

    /**
     * 允许延迟天数，逾期＝今天 > due_date + N
     */
    private Integer allowDelayDays;

    /**
     * 超收容差 %，M1 前端只读、默认 0（D25）
     */
    private BigDecimal overTolPct;

    /**
     * 发票类型（整单统一） VAT_INVOICE/NO_TAX
     */
    private String taxType;

    /**
     * 税率%（整单统一）
     */
    private BigDecimal taxRate;

    /**
     * 订单金额（不含运杂费）=Σ行 amount；故意不叫 total_amount，避免与执行单含运费口径混用（D26）
     */
    private BigDecimal goodsAmount;

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
