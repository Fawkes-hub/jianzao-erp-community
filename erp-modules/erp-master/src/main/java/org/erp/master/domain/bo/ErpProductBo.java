package org.erp.master.domain.bo;

import org.erp.master.domain.ErpProduct;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品业务对象
 */
@AutoMapper(target = ErpProduct.class, reverseConvertGenerate = false)
@Data
public class ErpProductBo {

    private Long id;

    private String code;

    @NotBlank(message = "名称不能为空")
    private String name;

    private String spec;

    @NotBlank(message = "单位不能为空")
    private String unit;

    @NotBlank(message = "类型不能为空")
    private String type;

    /**
     * 商品分类ID（空=未分类）
     */
    private Long categoryId;

    /**
     * 所属供应商ID（空=未指定）
     */
    private Long supplierId;

    /**
     * 数量精度位数 0~3（0=整数，DOC/26 §10）；为空时新增按数据库默认 0、修改保持原值
     */
    @Min(value = 0, message = "数量精度只能为 0~3 位")
    @Max(value = 3, message = "数量精度只能为 0~3 位")
    private Integer qtyScale;

    private BigDecimal costPrice;

    private BigDecimal purchasePrice;

    private String taxType;

    private BigDecimal freight;

    /**
     * 库存下限（可空=不启用，需 ≤ maxStock）
     */
    private BigDecimal minStock;

    /**
     * 库存上限（可空=不启用）
     */
    private BigDecimal maxStock;

    private Integer status;

    private String remark;
}
