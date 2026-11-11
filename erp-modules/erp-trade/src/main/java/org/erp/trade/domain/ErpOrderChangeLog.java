package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

/**
 * 订单变更留痕 biz_order_change_log（采购/销售订单共用，DOC/31 §5.5.4）
 *
 * 只增不删：留痕用于审计「谁把交期/数量/容差/送货地址改了」，不做更新与物理删除。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_order_change_log")
public class ErpOrderChangeLog extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    /**
     * 订单类型：PURCHASE/SALE
     */
    private String orderType;

    private Long orderId;

    /**
     * 订单号快照
     */
    private String orderNo;

    /**
     * 订单行ID，头级变更为空
     */
    private Long itemId;

    /**
     * 变更字段：qty/price/due_date/promised_date/closed_qty/closed_flag/close_reason/buyer/
     * supplier_order_no/customer_order_no/allow_delay_days/over_tol_pct/contact_name/contact_phone/
     * delivery_address/status
     */
    private String fieldKey;

    /**
     * 原值（字符串化；日期 yyyy-MM-dd，数量 3 位小数）
     */
    private String oldValue;

    /**
     * 新值（字符串化；地址最长 255，故列宽 VARCHAR(255)）
     */
    private String newValue;

    /**
     * 变更原因；qty/due_date/allow_delay_days/over_tol_pct/closed_qty/close_reason/delivery_address 必填
     */
    private String reason;

    private Long operatorId;

    /**
     * 操作人姓名快照
     */
    private String operatorName;

    private Date operateTime;

    @TableLogic
    private String delFlag;
}
