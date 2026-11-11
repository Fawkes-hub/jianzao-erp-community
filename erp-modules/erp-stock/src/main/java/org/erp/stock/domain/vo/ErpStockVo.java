package org.erp.stock.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存余额视图对象
 */
@Data
public class ErpStockVo {

    private Long productId;

    private String productCode;

    private String productName;

    private String spec;

    private String unit;

    private String type;

    /**
     * 数量精度 0~3（取商品 md_product.qty_scale），移动端据此格式化库存数量
     */
    private Integer qtyScale;

    /**
     * 库存下限（取商品 md_product.min_stock），为空表示未设置预警
     */
    private BigDecimal minStock;

    /**
     * 库存上限（取商品 md_product.max_stock），为空表示未设置预警
     */
    private BigDecimal maxStock;

    private Long warehouseId;

    private BigDecimal qty;

    private BigDecimal avgCost;

    private BigDecimal stockAmount;

    /**
     * 成本缺失：有库存但移动加权均价为 0/空（DOC/26 §2.4 防线③），前端据此标「成本缺失」
     */
    private Boolean costMissing;
}
