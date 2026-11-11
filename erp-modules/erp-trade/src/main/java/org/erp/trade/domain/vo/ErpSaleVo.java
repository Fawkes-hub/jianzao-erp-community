package org.erp.trade.domain.vo;

import lombok.Data;
import org.erp.trade.domain.bo.ErpSaleBo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 销售单视图对象
 */
@Data
public class ErpSaleVo {

    private Long id;

    private String no;

    /**
     * 来源销售订单ID，NULL=直开出库单
     */
    private Long sourceOrderId;

    /**
     * 来源销售订单号快照（详情页「本单来自订单 xxx」）
     */
    private String sourceOrderNo;

    /**
     * 客户联系人快照
     */
    private String contactName;

    /**
     * 客户联系电话快照
     */
    private String contactPhone;

    /**
     * 送货地址快照
     */
    private String deliveryAddress;

    private Long customerId;

    private String customerName;

    private Date bizDate;

    private String status;

    /**
     * 状态中文文案（后端回填，见 org.erp.common.enums.StatusText）
     */
    private String statusText;

    private String taxType;

    private BigDecimal taxRate;

    private BigDecimal goodsAmount;

    private BigDecimal freightAmount;

    private String freightAllocMethod;

    private BigDecimal totalAmount;

    private BigDecimal costAmount;

    private BigDecimal grossProfit;

    private BigDecimal grossMargin;

    private String remark;

    private Date createTime;

    private List<ItemVo> items;

    /**
     * 确认时成本为 0 的商品名（软提示，不阻断确认，DOC/26 §2.4）
     */
    private List<String> costWarnings;

    @Data
    public static class ItemVo {

        private Long id;

        /**
         * 来源销售订单行ID，NULL=直开出库单明细
         */
        private Long sourceOrderItemId;

        private Long productId;

        /**
         * 现录现卖快照：草稿阶段 productId=0；确认后商品已回填仍返回，撤回再编辑保存靠它恢复 QUICK_IN 入库
         */
        private ErpSaleBo.NewProduct newProduct;

        private String productCode;

        private String productName;

        private String spec;

        private String unit;

        private BigDecimal qty;

        /**
         * 数量精度 0~3（DOC/26 §10），前端据此设置数量输入精度
         */
        private Integer qtyScale;

        private BigDecimal price;

        private String taxType;

        private BigDecimal taxRate;

        private BigDecimal amount;

        private BigDecimal unitCost;

        private BigDecimal costAmount;

        private BigDecimal grossProfit;

        private String remark;
    }
}
