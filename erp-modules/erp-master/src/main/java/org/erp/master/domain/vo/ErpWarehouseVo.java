package org.erp.master.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.erp.master.domain.ErpWarehouse;

import java.util.Date;

/**
 * 仓库视图对象
 */
@Data
@AutoMapper(target = ErpWarehouse.class)
public class ErpWarehouseVo {

    private Long id;

    private String code;

    private String name;

    private Integer status;

    private String remark;

    private Date createTime;
}
