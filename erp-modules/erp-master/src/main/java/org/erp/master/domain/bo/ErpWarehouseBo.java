package org.erp.master.domain.bo;

import org.erp.master.domain.ErpWarehouse;

import jakarta.validation.constraints.NotBlank;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 仓库业务对象
 */
@AutoMapper(target = ErpWarehouse.class, reverseConvertGenerate = false)
@Data
public class ErpWarehouseBo {

    private Long id;

    private String code;

    @NotBlank(message = "名称不能为空")
    private String name;

    private Integer status;

    private String remark;
}
