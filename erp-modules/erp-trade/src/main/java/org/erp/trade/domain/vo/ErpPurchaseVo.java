package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购单视图对象
 */
@Data
public class ErpPurchaseVo {

    private Long id;

    private String no;

    /**
     * 来源采购订单ID，NULL=直开入库单
     */
    private Long sourceOrderId;

    /**
     * 来源采购订单号快照（详情页「本单来自订单 xxx」）
     */
    private String sourceOrderNo;

    /**
     * 供应商联系人快照
     */
    private String contactName;

    /**
     * 供应商联系电话快照
     */
    private String contactPhone;

    private Long supplierId;

    private String supplierName;

    private Date bizDate;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private String taxType;

    private BigDecimal taxRate;

    private BigDecimal goodsAmount;

    private BigDecimal freightAmount;

    private String freightAllocMethod;

    private BigDecimal totalAmount;

    private String remark;

    private Date createTime;

    private List<ItemVo> items;

    @Data
    public static class ItemVo {

        private Long id;

        /**
         * 来源采购订单行ID，NULL=直开入库单明细
         */
        private Long sourceOrderItemId;

        private Long productId;

        private String productCode;

        private String productName;

        private String spec;

        private String unit;

        private BigDecimal qty;

        /**
         * 数量精度 0~3（DOC/26 §10），前端据此设置数量输入精度
         */
        private Integer qtyScale;

        private BigDecimal price;

        private String taxType;

        private BigDecimal taxRate;

        private BigDecimal amount;

        private BigDecimal freightAmount;

        private BigDecimal freightPerUnit;

        private BigDecimal costPrice;

        private String remark;
    }
}
