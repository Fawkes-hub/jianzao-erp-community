package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 付款单 VO
 */
@Data
public class FinPaymentVo {

    private Long id;

    private String no;

    private Long supplierId;

    private String supplierName;

    private BigDecimal amount;

    private Date bizDate;

    private String account;

    /**
     * 结算账户ID（存量单为空）
     */
    private Long accountId;

    private String remark;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private Date createTime;

    private List<AllocVo> allocations = new ArrayList<>();

    @Data
    public static class AllocVo {

        private Long id;

        private Long arApId;

        private BigDecimal amount;
    }
}
