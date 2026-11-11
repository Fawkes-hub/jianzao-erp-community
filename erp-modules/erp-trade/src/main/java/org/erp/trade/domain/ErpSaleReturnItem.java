package org.erp.trade.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 销售退货明细 biz_sale_return_item（DOC/24 §2.2）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_sale_return_item")
public class ErpSaleReturnItem extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long orderId;

    private Long saleItemId;

    private Long productId;

    private BigDecimal qty;

    /**
     * 退货单价（默认=原行 price，可改）
     */
    private BigDecimal price;

    private BigDecimal amount;

    /**
     * 成本快照 = 原行 unit_cost，确认时回填
     */
    private BigDecimal unitCost;

    /**
     * 成本回冲额 = qty × unit_cost，确认时回填
     */
    private BigDecimal returnCost;

    private String remark;

    private Integer seq;

    /**
     * 逻辑删除标志：0存在 1删除；删除退货草稿时明细一并逻辑删除
     */
    @TableLogic
    private String delFlag;
}
