package org.erp.fin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * 结算账户 fin_account（DOC/25）
 *
 * 收付款单/其他收支的账户主数据；余额 = 期初 + Σ收 − Σ付，实时计算不落表。
 * 类型：CASH现金/BANK银行/WECHAT微信/ALIPAY支付宝/OTHER其他。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_account")
public class FinAccount extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String code;

    private String name;

    /**
     * CASH/BANK/WECHAT/ALIPAY/OTHER
     */
    private String type;

    /**
     * 期初余额（动账发生前可修改）
     */
    private BigDecimal openingBalance;

    /**
     * 1启用 0停用
     */
    private Integer status;

    private String remark;

    @TableLogic
    private String delFlag;
}
