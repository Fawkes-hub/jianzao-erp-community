package org.erp.master.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 产品导入行（DOC/22 §3.1）。
 *
 * <p>类型、发票类型、状态同时接受中文与原始码。</p>
 */
@Data
public class ProductImportRow {

    @ExcelProperty("产品编码")
    private String code;

    @ExcelProperty("产品名称")
    private String name;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("类型")
    private String type;

    /**
     * 商品分类编码，按 code 匹配 md_product_category（DOC/26 §4.6；名称可重名，编码唯一）
     */
    @ExcelProperty("商品分类编码")
    private String categoryCode;

    /**
     * 所属供应商编码，按 code 匹配 md_supplier
     */
    @ExcelProperty("供应商编码")
    private String supplierCode;

    /**
     * 数量精度：整数 / 1位 / 2位 / 3位，也接受 0~3 原始码（DOC/26 §10）
     */
    @ExcelProperty("数量精度")
    private String qtyScale;

    @ExcelProperty("参考成本")
    private String costPrice;

    @ExcelProperty("采购价")
    private String purchasePrice;

    @ExcelProperty("发票类型")
    private String taxType;

    @ExcelProperty("运费")
    private String freight;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
