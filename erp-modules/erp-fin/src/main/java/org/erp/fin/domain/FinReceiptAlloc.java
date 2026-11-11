package org.erp.fin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 收款核销明细 fin_receipt_alloc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_receipt_alloc")
public class FinReceiptAlloc extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long receiptId;

    private Long arApId;

    private BigDecimal amount;

    /**
     * 逻辑删除标志：0存在 1删除
     *
     * 注意：本表无业务唯一键，逻辑删除后重插同 (receipt_id, ar_ap_id) 行不会冲突。
     */
    @TableLogic
    private String delFlag;
}
