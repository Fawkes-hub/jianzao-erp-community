package org.erp.fin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 期初往来 fin_opening
 *
 * party_type+party_id 唯一；确认后落 fin_ar_ap(source_type=OPENING)（D1）并锁定。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_opening")
public class FinOpening extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String partyType;

    private Long partyId;

    private BigDecimal amount;

    private String status;

    private String remark;
}
