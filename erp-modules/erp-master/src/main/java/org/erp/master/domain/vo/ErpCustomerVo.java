package org.erp.master.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.erp.master.domain.ErpCustomer;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户视图对象
 */
@Data
@AutoMapper(target = ErpCustomer.class)
public class ErpCustomerVo {

    private Long id;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String address;

    /**
     * 期初欠款：仅为兼容档案字段保留，界面改读台账 OPENING 汇总（fin_ar_ap），不再作为录入入口。
     */
    private BigDecimal openingDebt;

    private String taxNo;

    private BigDecimal taxRate;

    private Integer status;

    private String remark;

    private Date createTime;
}
