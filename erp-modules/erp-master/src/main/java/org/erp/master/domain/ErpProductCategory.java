package org.erp.master.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 商品分类 md_product_category（DOC/26 §4.2）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_product_category")
public class ErpProductCategory extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String code;

    private String name;

    /**
     * 排序（升序），商品列表折叠分组按此排序
     */
    private Integer sort;

    /**
     * 1启用 0停用
     */
    private Integer status;

    private String remark;

    @TableLogic
    private String delFlag;
}
