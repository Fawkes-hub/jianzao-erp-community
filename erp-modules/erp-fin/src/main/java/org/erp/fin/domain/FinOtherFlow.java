package org.erp.fin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 其他收支单 fin_other_flow
 *
 * SR=其他收入 ZC=其他支出；独立于应收应付（R19）；
 * 支出 affect_cost=1 时确认写 fin_cost_ledger(OTHER_EXPENSE)（S7/R20）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_other_flow")
public class FinOtherFlow extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String no;

    /**
     * INCOME / EXPENSE
     */
    private String flowType;

    private String category;

    private BigDecimal amount;

    private Date bizDate;

    private String account;

    /**
     * 结算账户ID（DOC/25：新单必填，存量单为空沿用 account 文本）
     */
    private Long accountId;

    /**
     * 是否计入综合成本（仅支出有效）
     */
    private Integer affectCost;

    private String remark;

    private String status;

    private Long confirmedBy;

    private Date confirmedAt;

    private Long withdrawnBy;

    private Date withdrawnAt;

    private Long voidedBy;

    private Date voidedAt;

    /**
     * 逻辑删除标志：0存在 1删除（草稿删除只置位，不物理删行，单号不回收）
     */
    @TableLogic
    private String delFlag;
}
