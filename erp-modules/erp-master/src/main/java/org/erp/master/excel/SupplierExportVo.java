package org.erp.master.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.erp.common.excel.enums.StatusEnum;

import java.math.BigDecimal;

/**
 * 供应商导出/模板行（DOC/22 §3.3）。不含期初应付。
 */
@Data
public class SupplierExportVo {

    @ExcelNotation("留空时按供应商名称自动生成；编码/电话请使用文本格式，避免前导 0 丢失")
    @ExcelProperty("供应商编码")
    private String code;

    @ExcelRequired
    @ExcelNotation("必填")
    @ExcelProperty("供应商名称")
    private String name;

    @ExcelNotation("可选")
    @ExcelProperty("联系人")
    private String contact;

    @ExcelNotation("建议使用文本格式，保留前导 0")
    @ExcelProperty("电话")
    private String phone;

    @ExcelNotation("可选")
    @ExcelProperty("地址")
    private String address;

    @ExcelNotation("文本格式")
    @ExcelProperty("税号")
    private String taxNo;

    @ExcelNotation("0~100，最多 4 位小数；留空默认 0")
    @ExcelProperty("默认税率%")
    private BigDecimal taxRate;

    @ExcelEnumFormat(enumClass = StatusEnum.class)
    @ExcelNotation("可选：启用 / 停用；留空默认启用")
    @ExcelProperty("状态")
    private String status;

    @ExcelNotation("可选，最多 500 字")
    @ExcelProperty("备注")
    private String remark;
}
