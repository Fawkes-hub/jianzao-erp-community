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

    private BigDecimal openingDebt;

    private String taxNo;

    private BigDecimal taxRate;

    private Integer status;

    private String remark;

    private Date createTime;
}
