package org.erp.trade.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 其他出库 Bo
 */
@Data
public class ErpOtherOutBo {

    @NotNull(message = "业务日期不能为空")
    private Date bizDate;

    @NotBlank(message = "成本分类不能为空")
    @Pattern(regexp = "PRODUCTION_CONSUMABLE|OFFICE|MAINTENANCE|OTHER",
        message = "成本分类必须是 PRODUCTION_CONSUMABLE/OFFICE/MAINTENANCE/OTHER")
    private String costCategory;

    private String remark;

    /**
     * true = 保存并确认
     */
    private Boolean confirm;

    @Valid
    @NotEmpty(message = "出库明细不能为空")
    private List<ItemBo> items = new ArrayList<>();

    @Data
    public static class ItemBo {

        @NotNull(message = "产品不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Positive(message = "数量必须大于0")
        private BigDecimal qty;

        private String remark;
    }
}
