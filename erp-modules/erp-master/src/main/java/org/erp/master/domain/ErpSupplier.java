package org.erp.master.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 供应商 md_supplier
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_supplier")
public class ErpSupplier extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String address;

    /**
     * 期初应付（仅录入展示，账面以 fin_opening/fin_ar_ap 为准）
     */
    private BigDecimal openingPayable;

    private String taxNo;

    private BigDecimal taxRate;

    private Integer status;

    private String remark;

    @TableLogic
    private String delFlag;
}
