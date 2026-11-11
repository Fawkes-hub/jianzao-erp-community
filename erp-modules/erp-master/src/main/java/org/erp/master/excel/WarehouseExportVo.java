package org.erp.master.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.erp.common.excel.enums.StatusEnum;

/**
 * 仓库导出/模板行（DOC/22 §3.3）。
 */
@Data
public class WarehouseExportVo {

    @ExcelNotation("留空时按仓库名称自动生成")
    @ExcelProperty("仓库编码")
    private String code;

    @ExcelRequired
    @ExcelNotation("必填")
    @ExcelProperty("仓库名称")
    private String name;

    @ExcelEnumFormat(enumClass = StatusEnum.class)
    @ExcelNotation("可选：启用 / 停用；留空默认启用")
    @ExcelProperty("状态")
    private String status;

    @ExcelNotation("可选，最多 500 字")
    @ExcelProperty("备注")
    private String remark;
}
