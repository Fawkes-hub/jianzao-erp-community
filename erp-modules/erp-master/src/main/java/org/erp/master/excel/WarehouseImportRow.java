package org.erp.master.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 仓库导入行（DOC/22 §3.1）。
 */
@Data
public class WarehouseImportRow {

    @ExcelProperty("仓库编码")
    private String code;

    @ExcelProperty("仓库名称")
    private String name;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
