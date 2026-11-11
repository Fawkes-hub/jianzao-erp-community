package org.erp.fin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成本台账 fin_cost_ledger
 *
 * cost_type: PRODUCTION_MATERIAL（生产材料，报表参考口径）/OTHER_OUT/OTHER_EXPENSE；
 * 综合成本 = 销售成本 + OTHER_OUT + OTHER_EXPENSE；
 * status: ACTIVE/REVERSED（撤回置 REVERSED，不删行）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_cost_ledger")
public class FinCostLedger extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String costType;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private Date bizDate;

    private BigDecimal amount;

    private String status;

    private String remark;
}
