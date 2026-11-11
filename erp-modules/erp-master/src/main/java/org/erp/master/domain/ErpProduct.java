package org.erp.master.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 产品/原料 md_product
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_product")
public class ErpProduct extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String code;

    private String name;

    private String spec;

    private String unit;

    /**
     * PRODUCT=成品 MATERIAL=原料
     */
    private String type;

    /**
     * 商品分类ID（NULL=未分类，DOC/26 §4.1）
     */
    private Long categoryId;

    /**
     * 所属供应商ID（NULL=未指定，DOC/26 §4.3）
     */
    private Long supplierId;

    /**
     * 数量精度位数 0~3（0=整数，DOC/26 §10）；存储仍为 DECIMAL(18,3)
     */
    private Integer qtyScale;

    /**
     * 当前参考成本单价（含分摊运杂费）
     */
    private BigDecimal costPrice;

    private BigDecimal purchasePrice;

    /**
     * NO_TAX/VAT_INVOICE/GENERAL_INVOICE
     */
    private String taxType;

    private BigDecimal freight;

    /**
     * 库存下限（NULL=不启用预警，DOC/25）
     */
    private BigDecimal minStock;

    /**
     * 库存上限（NULL=不启用预警，DOC/25）
     */
    private BigDecimal maxStock;

    /**
     * 1启用 0停用
     */
    private Integer status;

    private String remark;

    @TableLogic
    private String delFlag;
}
