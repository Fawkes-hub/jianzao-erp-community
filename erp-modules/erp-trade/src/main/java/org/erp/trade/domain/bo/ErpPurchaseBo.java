package org.erp.trade.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购单业务对象
 */
@Data
public class ErpPurchaseBo {

    private Long id;

    /**
     * 来源采购订单ID（转单生成时写入；直开单为空）。
     * 只入头部，联系人等快照由后端从订单取，避免编辑时被前端冲成 null
     */
    private Long sourceOrderId;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

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

    /**
     * confirm=true 时保存并确认
     */
    private Boolean confirm;

    @NotEmpty(message = "明细不能为空")
    private List<PurchaseItemBo> items;

    @Data
    public static class PurchaseItemBo {

        @NotNull(message = "产品不能为空")
        private Long productId;

        /**
         * 来源采购订单行ID（转单生成时写入；直开单明细为空）
         */
        private Long sourceOrderItemId;

        @NotNull(message = "数量不能为空")
        private BigDecimal qty;

        @NotNull(message = "单价不能为空")
        private BigDecimal price;

        private String remark;
    }
}
