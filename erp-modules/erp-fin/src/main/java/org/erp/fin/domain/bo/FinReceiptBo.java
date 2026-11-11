package org.erp.fin.domain.bo;

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
 * 收款单 Bo（新建/编辑，confirm=true 时直接确认核销）
 */
@Data
public class FinReceiptBo {

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "收款日期不能为空")
    private Date bizDate;

    @NotNull(message = "收款金额不能为空")
    @Positive(message = "收款金额必须大于0")
    private BigDecimal amount;

    private String account;

    /**
     * 结算账户ID（新增必填；存量草稿编辑放行空值，DOC/25 §3.6）
     */
    private Long accountId;

    private String remark;

    /**
     * 红字标志（DOC/24）：仅退货退款内部链路使用，普通收款页面不传
     */
    private Boolean negative;

    /**
     * true = 保存并确认（落核销）
     */
    private Boolean confirm;

    @Valid
    private List<AllocationBo> allocations = new ArrayList<>();

    @Data
    public static class AllocationBo {

        @NotNull(message = "台账ID不能为空")
        private Long arApId;

        @NotNull(message = "核销金额不能为空")
        @Positive(message = "核销金额必须大于0")
        private BigDecimal amount;
    }
}
