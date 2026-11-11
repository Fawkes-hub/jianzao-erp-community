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
 * 采购退货 Bo（DOC/24 §2.0，无退款字段）
 */
@Data
public class ErpPurchaseReturnBo {

    @NotNull(message = "原采购单不能为空")
    private Long purchaseId;

    @NotNull(message = "业务日期不能为空")
    private Date bizDate;

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

        @NotNull(message = "原采购明细行不能为空")
        private Long purchaseItemId;

        @NotNull(message = "退货数量不能为空")
        @Positive(message = "退货数量必须大于0")
        private BigDecimal qty;

        @NotNull(message = "退货单价不能为空")
        @Positive(message = "退货单价必须大于0")
        private BigDecimal price;

        private String remark;
    }
}
