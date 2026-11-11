package org.erp.master.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 仓库 md_warehouse
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_warehouse")
public class ErpWarehouse extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String code;

    private String name;

    private Integer status;

    private String remark;

    @TableLogic
    private String delFlag;
}
