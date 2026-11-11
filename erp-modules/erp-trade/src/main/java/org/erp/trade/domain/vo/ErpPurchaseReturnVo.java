package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 采购退货 VO（DOC/24 §2.0）
 */
@Data
public class ErpPurchaseReturnVo {

    private Long id;

    private String no;

    private Long purchaseId;

    private String purchaseNo;

    private Long supplierId;

    private String supplierName;

    private Date bizDate;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private BigDecimal totalAmount;

    private BigDecimal returnCostAmount;

    private String remark;

    private Date createTime;

    private List<ItemVo> items = new ArrayList<>();

    @Data
    public static class ItemVo {

        private Long id;

        private Long purchaseItemId;

        private Long productId;

        private String productCode;

        private String productName;

        private String spec;

        private String unit;

        private BigDecimal qty;

        /**
         * 数量精度 0~3（DOC/26 §10），前端据此设置退货数量输入精度
         */
        private Integer qtyScale;

        private BigDecimal price;

        private BigDecimal amount;

        private BigDecimal unitCost;

        private BigDecimal returnCost;

        private String remark;

        private BigDecimal originalQty;

        private BigDecimal returnedQty;

        private BigDecimal returnableQty;
    }
}
