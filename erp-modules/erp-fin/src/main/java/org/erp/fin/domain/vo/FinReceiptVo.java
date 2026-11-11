package org.erp.fin.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 收款单 VO
 */
@Data
public class FinReceiptVo {

    private Long id;

    private String no;

    private Long customerId;

    private String customerName;

    private BigDecimal amount;

    private Date bizDate;

    private String account;

    /**
     * 结算账户ID（存量单为空）
     */
    private Long accountId;

    /**
     * 红字标志：1=退款单（前端据此显示退款徽标与负值金额）
     */
    private Integer negative;

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
