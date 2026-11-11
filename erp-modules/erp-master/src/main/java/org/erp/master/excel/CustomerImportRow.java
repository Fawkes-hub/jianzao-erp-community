package org.erp.master.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 客户导入行（DOC/22 §3.1）。
 *
 * <p>所有字段按文本接收，再交给 {@link org.erp.common.excel.CellValues} 校验，
 * 避免 Excel 数值单元格丢失前导 0 或精度。</p>
 */
@Data
public class CustomerImportRow {

    @ExcelProperty("客户编码")
    private String code;

    @ExcelProperty("客户名称")
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
