package org.erp.trade.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 销售订单业务对象（DOC/31 §5.5.3、§6.3）
 *
 * <p>与采购订单 BO 同构，差异只有客户/客户单号/跟单人命名与送货地址。
 * 已确认订单改单时，items 按行 id 与库内订单行一一对应，不允许增删行。
 */
@Data
public class ErpSaleOrderBo {

    private Long id;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "下单日期不能为空")
    private Date bizDate;

    /**
     * 客户单号（对方单号），最长 64
     */
    private String customerOrderNo;

    /**
     * 跟单人 sys_user.id，不传默认当前登录人
     */
    private Long salespersonId;

    /**
     * 跟单人姓名快照，不传且跟单人为当前登录人时取登录名
     */
    private String salespersonName;

    /**
     * 客户联系人，最长 64
     */
    private String contactName;

    /**
     * 客户联系电话，最长 32
     */
    private String contactPhone;

    /**
     * 送货地址，最长 255；变更时 reason 必填
     */
    private String deliveryAddress;

    /**
     * 允许延迟天数，默认 0
     */
    private Integer allowDelayDays;

    /**
     * 超发容差 %，默认 0（M1 前端只读，建单可写）
     */
    private BigDecimal overTolPct;

    /**
     * 发票类型（整单统一），默认 VAT_INVOICE
     */
    private String taxType;

    /**
     * 税率%（整单统一），默认 13
     */
    private BigDecimal taxRate;

    private String remark;

    /**
     * 变更原因：qty/due_date/allow_delay_days/over_tol_pct/closed_qty/close_reason/delivery_address 变更时必填，最长 200
     */
    private String reason;

    @NotEmpty(message = "明细不能为空")
    @Valid
    private List<ItemBo> items;

    /**
     * 订单行；已确认订单改单时 id 必填且必须属于本单。
     */
    @Data
    public static class ItemBo {

        /**
         * 订单行ID：草稿新增时为空，已确认改单时必填
         */
        private Long id;

        private Long productId;

        private BigDecimal qty;

        private BigDecimal price;

        /**
         * 约定交期，必填
         */
        private Date dueDate;

        /**
         * 承诺交期，选填，不参与逾期判定
         */
        private Date promisedDate;

        private String remark;
    }

    /**
     * 关闭剩余（整单/单行）入参：原因必填。
     */
    @Data
    public static class CloseBo {

        /**
         * 关闭原因，必填，最长 200
         */
        private String reason;
    }

    /**
     * 订单转销售出库单入参（POST /{id}/deliveries）。
     */
    @Data
    public static class DeliveryBo {

        /**
         * 出库日期，不传取当前日期
         */
        private Date bizDate;

        /**
         * 本次出库行；不传默认带全部未交行、数量＝remain
         */
        private List<DeliveryItemBo> items;

        private String remark;
    }

    /**
     * 转单行：订单行ID + 本次数量。
     */
    @Data
    public static class DeliveryItemBo {

        private Long orderItemId;

        private BigDecimal qty;
    }
}
