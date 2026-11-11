package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购订单明细 biz_purchase_order_item（DOC/31 §5.5.2）
 *
 * <p>字段与销售订单行完全一致：订单行不支持现录现卖，没有 new_product_snapshot。
 * executed_qty 由 recalc 覆盖写；closed_qty 在关闭那一刻冻结，重算只做下界保护（D7）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_purchase_order_item")
public class ErpPurchaseOrderItem extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    /**
     * 采购订单ID
     */
    private Long orderId;

    /**
     * 同单内顺序（0起），仅排序，不作唯一键
     */
    private Integer seq;

    /**
     * 产品ID（订单行不支持现录现卖）
     */
    private Long productId;

    /**
     * 订单量（承诺量）>0
     */
    private BigDecimal qty;

    /**
     * 采购单价（含税/不含税由 tax_type 决定，与执行单一致）
     */
    private BigDecimal price;

    /**
     * 发票类型快照（建单时继承表头）
     */
    private String taxType;

    /**
     * 税率%快照（建单时继承表头）
     */
    private BigDecimal taxRate;

    /**
     * 金额=数量×单价，保留 2 位
     */
    private BigDecimal amount;

    /**
     * 约定交期（我方向供应商要求的到货日），必填
     */
    private Date dueDate;

    /**
     * 供应商回签承诺交期（不参与逾期判定）
     */
    private Date promisedDate;

    /**
     * 累计已执行净额（Σ已确认入库 − Σ已确认退货），重算覆盖写
     */
    private BigDecimal executedQty;

    /**
     * 手工关闭量（冻结；closed_flag=1 时手工写入，recalc 只做下界保护）
     */
    private BigDecimal closedQty;

    /**
     * 1=已关闭剩余
     */
    private Integer closedFlag;

    /**
     * 关闭原因（closed_flag=1 时必填）
     */
    private String closeReason;

    private Long closedBy;

    private Date closedAt;

    private String remark;

    @TableLogic
    private String delFlag;
}
