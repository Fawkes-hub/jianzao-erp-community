package org.erp.trade.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 销售退货 Bo（DOC/24 §2.0）
 *
 * negative 由后端写死、不经 BO 传入（防篡改）。
 */
@Data
public class ErpSaleReturnBo {

    @NotNull(message = "原销售单不能为空")
    private Long saleId;

    @NotNull(message = "业务日期不能为空")
    private Date bizDate;

    /**
     * 是否退款给客户
     */
    private Boolean needRefund;

    /**
     * 退款金额（≤退货总额，≤原单已收款）
     */
    private BigDecimal refundAmount;

    /**
     * 退款账户（need_refund=1 时必填，落红字收款单 fin_receipt.account）
     */
    private Long refundAccountId;

    private String remark;

    /**
     * true = 保存并确认
     */
    private Boolean confirm;

    @Valid
    @NotEmpty(message = "退货明细不能为空")
    private List<ItemBo> items = new ArrayList<>();

    @Data
    public static class ItemBo {

        @NotNull(message = "原销售明细行不能为空")
        private Long saleItemId;

        @NotNull(message = "退货数量不能为空")
        @Positive(message = "退货数量必须大于0")
        private BigDecimal qty;

        /**
         * 退货单价（默认=原行价格，可改）
         */
        @NotNull(message = "退货单价不能为空")
        @Positive(message = "退货单价必须大于0")
        private BigDecimal price;

        private String remark;
    }
}
