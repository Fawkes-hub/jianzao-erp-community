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
 * 其他入库单 biz_other_in
 *
 * QR 单号；确认按录入单价入库（OTHER_IN）、移动加权更新成本；不产生应收应付。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_other_in")
public class ErpOtherIn extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String no;

    private Date bizDate;

    private String status;

    /**
     * STOCKTAKING_IN/REWORK_IN/GIFT_IN/TRANSFER_IN/RETURN_IN/OTHER
     */
    private String reason;

    private BigDecimal amount;

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
