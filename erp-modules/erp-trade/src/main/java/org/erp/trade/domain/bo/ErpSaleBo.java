package org.erp.trade.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 销售单业务对象
 */
@Data
public class ErpSaleBo {

    private Long id;

    /**
     * 来源销售订单ID（转单生成时写入；直开单为空）。
     * 只入头部，联系人/送货地址由后端从订单取，避免编辑时被前端冲成 null
     */
    private Long sourceOrderId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "日期不能为空")
    private Date bizDate;

    /**
     * 发票类型（整单统一），默认 VAT_INVOICE
     */
    private String taxType;

    /**
     * 税率%（整单统一），默认 13
     */
    private BigDecimal taxRate;

    /**
     * 运杂费总额
     */
    private BigDecimal freightAmount;

    /**
     * 分摊方式已固定按数量，保留字段兼容旧调用，后端忽略传入值
     */
    private String freightAllocMethod;

    private String remark;

    private Boolean confirm;

    @NotEmpty(message = "明细不能为空")
    @Valid
    private List<SaleItemBo> items;

    @Data
    public static class SaleItemBo {

        private Long productId;

        /**
         * 来源销售订单行ID（转单生成时写入；直开单明细为空）
         */
        private Long sourceOrderItemId;

        private BigDecimal qty;

        /**
         * 现录现卖时随单新建的产品资料
         */
        @Valid
        private NewProduct newProduct;

        private BigDecimal price;

        private String remark;
    }

    @Data
    public static class NewProduct {

        private String code;

        private String name;

        private String spec;

        private String unit;

        private BigDecimal costPrice;

        private String taxType;

        /**
         * 数量精度位数 0~3（0=整数，DOC/26 §10），空值按 0
         */
        @Min(value = 0, message = "数量精度不能小于 0")
        @Max(value = 3, message = "数量精度不能大于 3")
        private Integer qtyScale;

        /**
         * 入库数量（DOC/26 §8.3）：确认时按此数量入库，默认等于销售数量，可改大（多进的留在库存）
         */
        private BigDecimal qty;

        /**
         * 商品分类ID（空=未分类），建品时写入，与商品资料口径一致
         */
        private Long categoryId;

        /**
         * 所属供应商ID（空=未指定），建品时写入
         */
        private Long supplierId;

        /**
         * 默认采购单价，建品时写入
         */
        private BigDecimal purchasePrice;

        /**
         * 默认运杂费（元/单位），建品时写入
         */
        private BigDecimal freight;

        /**
         * 库存下限（空=不启用预警），建品时写入
         */
        private BigDecimal minStock;

        /**
         * 库存上限（空=不启用预警），建品时写入
         */
        private BigDecimal maxStock;

        /**
         * 商品备注（区别于销售明细行备注）
         */
        private String remark;
    }
}
