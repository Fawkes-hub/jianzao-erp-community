package org.erp.master.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 供应商导入行（DOC/22 §3.1）。不含期初应付。
 */
@Data
public class SupplierImportRow {

    @ExcelProperty("供应商编码")
    private String code;

    @ExcelProperty("供应商名称")
    private String name;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("电话")
    private String phone;

    @ExcelProperty("地址")
    private String address;

    @ExcelProperty("税号")
    private String taxNo;

    @ExcelProperty("默认税率%")
    private String taxRate;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
