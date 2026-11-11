package org.erp.fin.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算账户 Bo（DOC/25 §3.4）
 */
@Data
public class FinAccountBo {

    private Long id;

    @NotBlank(message = "账户编码不能为空")
    private String code;

    @NotBlank(message = "账户名称不能为空")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    @Pattern(regexp = "CASH|BANK|WECHAT|ALIPAY|OTHER", message = "账户类型必须是 CASH/BANK/WECHAT/ALIPAY/OTHER")
    private String type;

    /**
     * 期初余额，仅新增（或无动账）时可填/可改
     */
    private BigDecimal openingBalance;

    /**
     * 1启用 0停用
     */
    private Integer status;

    private String remark;
}
