package org.erp.stock.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存预警行 VO（DOC/25 §3.2）
 */
@Data
public class ErpStockAlertVo {

    private Long productId;

    private String productCode;

    private String productName;

    private String spec;

    private String unit;

    private BigDecimal minStock;

    private BigDecimal maxStock;

    /**
     * 该产品全部仓库 qty 合计（LEFT JOIN，无库存行按 0）
     */
    private BigDecimal totalQty;

    /**
     * 缺口：低于下限为 totalQty−minStock（负数），高于上限为 totalQty−maxStock（正数）
     */
    private BigDecimal gap;

    /**
     * LOW=低于下限 / HIGH=高于上限
     */
    private String alertType;
}
