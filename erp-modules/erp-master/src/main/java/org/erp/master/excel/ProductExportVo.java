package org.erp.master.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.erp.common.excel.enums.ProductTypeEnum;
import org.erp.common.excel.enums.QtyScaleEnum;
import org.erp.common.excel.enums.StatusEnum;
import org.erp.common.excel.enums.TaxTypeEnum;

import java.math.BigDecimal;

/**
 * 产品导出/模板行（DOC/22 §3.3）。类型与发票类型导出为中文。
 */
@Data
public class ProductExportVo {

    @ExcelNotation("留空时按产品名称自动生成；请使用文本格式")
    @ExcelProperty("产品编码")
    private String code;

    @ExcelRequired
    @ExcelNotation("必填")
    @ExcelProperty("产品名称")
    private String name;

    @ExcelNotation("可选")
    @ExcelProperty("规格")
    private String spec;

    @ExcelRequired
    @ExcelNotation("必填，如：个、件、kg")
    @ExcelProperty("单位")
    private String unit;

    @ExcelRequired
    @ExcelEnumFormat(enumClass = ProductTypeEnum.class)
    @ExcelNotation("可选：成品 / 原料")
    @ExcelProperty("类型")
    private String type;

    @ExcelNotation("可选，须填写商品分类编码（分类维护中查看）；留空=未分类（更新时留空保持原分类）")
    @ExcelProperty("商品分类编码")
    private String categoryCode;

    @ExcelNotation("可选，须填写供应商编码（供应商资料中查看）；留空=未指定（更新时留空保持原供应商）")
    @ExcelProperty("供应商编码")
    private String supplierCode;

    @ExcelEnumFormat(enumClass = QtyScaleEnum.class)
    @ExcelNotation("可选：整数 / 1位 / 2位 / 3位；留空默认整数（更新时留空保持原值）")
    @ExcelProperty("数量精度")
    private String qtyScale;

    @ExcelNotation(">=0，最多 4 位小数；仅参考，不参与库存成本")
    @ExcelProperty("参考成本")
    private BigDecimal costPrice;

    @ExcelNotation(">=0，最多 4 位小数")
    @ExcelProperty("采购价")
    private BigDecimal purchasePrice;

    @ExcelEnumFormat(enumClass = TaxTypeEnum.class)
    @ExcelNotation("可选：不含税 / 增值税发票 / 普通发票；留空默认不含税")
    @ExcelProperty("发票类型")
    private String taxType;

    @ExcelNotation(">=0，最多 4 位小数")
    @ExcelProperty("运费")
    private BigDecimal freight;

    @ExcelEnumFormat(enumClass = StatusEnum.class)
    @ExcelNotation("可选：启用 / 停用；留空默认启用")
    @ExcelProperty("状态")
    private String status;

    @ExcelNotation("可选，最多 500 字")
    @ExcelProperty("备注")
    private String remark;
}
