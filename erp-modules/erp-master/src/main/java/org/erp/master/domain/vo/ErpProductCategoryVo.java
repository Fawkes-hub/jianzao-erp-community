package org.erp.master.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.erp.master.domain.ErpProductCategory;

import java.util.Date;

/**
 * 商品分类视图对象
 */
@Data
@AutoMapper(target = ErpProductCategory.class)
public class ErpProductCategoryVo {

    private Long id;

    private String code;

    private String name;

    private Integer sort;

    private Integer status;

    private String remark;

    private Date createTime;
}
