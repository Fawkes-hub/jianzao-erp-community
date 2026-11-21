package org.erp.fin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /**
     * 期初截止日（含当日）：期初金额表示该日期之前的账面余额，
     * 确认时作为 fin_ar_ap.biz_date 落库，决定期初在哪个账期可见。
     */
    private LocalDate openingDate;

    private String status;

    private String remark;
}
