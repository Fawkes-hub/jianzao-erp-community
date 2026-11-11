package org.erp.master.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.erp.master.domain.ErpProduct;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 产品视图对象
 */
@Data
@AutoMapper(target = ErpProduct.class)
public class ErpProductVo {

    private Long id;

    private String code;

    private String name;

    private String spec;

    private String unit;

    private String type;

    /**
     * 商品分类ID（空=未分类）
     */
    private Long categoryId;

    /**
     * 商品分类名称（联查；分类被逻辑删除时为 null，前端归「未分类」）
     */
    private String categoryName;

    /**
     * 商品分类编码（联查，供 Excel 导出/导入按 code 匹配，DOC/26 §4.6）
     */
    private String categoryCode;

    /**
     * 商品分类排序（联查；前端折叠分组按此排序）
     */
    private Integer categorySort;

    /**
     * 所属供应商ID（空=未指定）
     */
    private Long supplierId;

    /**
     * 所属供应商名称（联查；供应商被删除时为 null）
     */
    private String supplierName;

    /**
     * 所属供应商编码（联查，供 Excel 导出/导入按 code 匹配，DOC/26 §4.6）
     */
    private String supplierCode;

    /**
     * 数量精度位数 0~3（0=整数，DOC/26 §10），前端据此切换数量输入精度
     */
    private Integer qtyScale;

    private BigDecimal costPrice;

    private BigDecimal purchasePrice;

    private String taxType;

    private BigDecimal freight;

    /**
     * 库存下限（可空=不启用预警）
     */
    private BigDecimal minStock;

    /**
     * 库存上限（可空=不启用预警）
     */
    private BigDecimal maxStock;

    private Integer status;

    private String remark;

    private Date createTime;
}
