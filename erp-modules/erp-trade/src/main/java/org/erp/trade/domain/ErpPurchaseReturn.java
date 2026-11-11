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
 * 采购退货单 biz_purchase_return（DOC/24 §2.3）
 *
 * 必须关联 CONFIRMED 原采购单；确认后库存回减（原行 cost_price 快照）、应付红冲（负数 AP 行）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_purchase_return")
public class ErpPurchaseReturn extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String no;

    private Long purchaseId;

    /**
     * 原单号快照
     */
    private String purchaseNo;

    private Long supplierId;

    private Date bizDate;

    private String status;

    private BigDecimal totalAmount;

    private BigDecimal returnCostAmount;

    private String remark;

    private Long confirmedBy;

    private Date confirmedAt;

    private Long withdrawnBy;

    private Date withdrawnAt;

    private Long voidedBy;

    private Date voidedAt;

    /**
     * 逻辑删除标志：0存在 1删除；草稿单据删除走逻辑删除，单号不回收
     */
    @TableLogic
    private String delFlag;
}
