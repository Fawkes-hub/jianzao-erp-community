package org.erp.master.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.erp.master.domain.ErpProductCategory;

/**
 * 商品分类业务对象
 */
@AutoMapper(target = ErpProductCategory.class, reverseConvertGenerate = false)
@Data
public class ErpProductCategoryBo {

    private Long id;

    private String code;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 排序（升序），留空按 0 处理
     */
    private Integer sort;

    private Integer status;

    private String remark;
}
