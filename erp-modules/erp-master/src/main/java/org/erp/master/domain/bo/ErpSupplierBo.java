package org.erp.master.domain.bo;

import org.erp.master.domain.ErpSupplier;

import jakarta.validation.constraints.NotBlank;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商业务对象
 */
@AutoMapper(target = ErpSupplier.class, reverseConvertGenerate = false)
@Data
public class ErpSupplierBo {

    private Long id;

    private String code;

    @NotBlank(message = "名称不能为空")
    private String name;

    private String contact;

    private String phone;

    private String address;

    private BigDecimal openingPayable;

    private String taxNo;

    private BigDecimal taxRate;

    private Integer status;

    private String remark;
}
