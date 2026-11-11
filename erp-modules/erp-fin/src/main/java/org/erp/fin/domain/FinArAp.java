package org.erp.fin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 应收应付台账 fin_ar_ap
 *
 * AR=应收（客户） AP=应付（供应商）；
 * source_type: SALE/PURCHASE/OPENING；
 * 状态机 OPEN → PARTIAL → SETTLED；作废置 VOID（settled_amount 必须为 0）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_ar_ap")
public class FinArAp extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String partyType;

    private Long partyId;

    private String sourceType;

    private Long sourceId;

    private Date bizDate;

    private BigDecimal amount;

    private BigDecimal settledAmount;

    private String status;
}
