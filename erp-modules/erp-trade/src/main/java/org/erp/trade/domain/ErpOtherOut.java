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
 * 其他出库单 biz_other_out
 *
 * QT 单号；确认扣库存（OTHER_OUT）、记成本（fin_cost_ledger）；不产生应收（S3/R17）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_other_out")
public class ErpOtherOut extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String no;

    private Date bizDate;

    private String status;

    /**
     * PRODUCTION_CONSUMABLE/OFFICE/MAINTENANCE/OTHER
     */
    private String costCategory;

    private BigDecimal costAmount;

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
