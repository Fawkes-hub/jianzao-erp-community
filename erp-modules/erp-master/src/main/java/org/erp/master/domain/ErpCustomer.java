package org.erp.master.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 客户 md_customer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_customer")
public class ErpCustomer extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String address;

    /**
     * 期初欠款：仅为兼容档案字段保留，账面只认 fin_opening/fin_ar_ap，
     * 界面改读台账 OPENING 汇总，不再作为录入入口。
     */
    private BigDecimal openingDebt;

    private String taxNo;

    private BigDecimal taxRate;

    private Integer status;

    private String remark;

    @TableLogic
    private String delFlag;
}
