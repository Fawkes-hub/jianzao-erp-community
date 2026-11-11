package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 其他出库单 VO
 */
@Data
public class ErpOtherOutVo {

    private Long id;

    private String no;

    private Date bizDate;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private String costCategory;

    private BigDecimal costAmount;

    private String remark;

    private Date createTime;

    private List<ItemVo> items = new ArrayList<>();

    @Data
    public static class ItemVo {

        private Long id;

        private Long productId;

        private String productCode;

        private String productName;

        private BigDecimal qty;

        /**
         * 数量精度 0~3（DOC/26 §10），前端据此设置数量输入精度
         */
        private Integer qtyScale;

        private BigDecimal unitCost;

        private BigDecimal amount;

        private String remark;
    }
}
