package org.erp.stock.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 库存流水 st_stock_log（只增不改；作废/撤回写反向流水）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("st_stock_log")
public class ErpStockLog extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    /**
     * OPENING/PURCHASE/PROD_IN/PROD_OUT/SALE/OTHER_OUT/OTHER_IN/CHECK/SALE_RETURN/PURCHASE_RETURN/QUICK_IN/VOID
     */
    private String bizType;

    private String bizNo;

    private Long bizId;

    private Long productId;

    private Long warehouseId;

    /**
     * 有符号数量：入正出负
     */
    private BigDecimal qty;

    private BigDecimal unitCost;

    private BigDecimal amount;

    /**
     * SUPPLIER/CUSTOMER/NONE
     */
    private String partnerType;

    private Long partnerId;

    private BigDecimal balanceAfter;

    private Date bizDate;

    private Integer seq;
}
