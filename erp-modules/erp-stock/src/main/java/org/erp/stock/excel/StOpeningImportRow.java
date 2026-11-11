package org.erp.stock.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;

/**
 * 期初库存导入行（DOC/22 §4.1）。
 *
 * <p>所有字段按文本接收，再交给 {@link org.erp.common.excel.CellValues} 做必填、
 * 小数位与数值校验，避免 Excel 数值单元格精度丢失。模板下载复用本类，
 * 保证模板表头与导入解析列完全一致。</p>
 */
@Data
public class StOpeningImportRow {

    @ExcelRequired
    @ExcelNotation("必填，必须是产品资料中已存在的产品编码")
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelNotation("必填，必须是仓库资料中已存在的仓库编码")
    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @ExcelRequired
    @ExcelNotation(">=0，最多 3 位小数")
    @ExcelProperty("期初数量")
    private String qty;

    @ExcelRequired
    @ExcelNotation(">=0，最多 4 位小数")
    @ExcelProperty("期初成本")
    private String unitCost;

    /**
     * 只读列：导出时 = qty × unitCost，导入时忽略。
     */
    @ExcelNotation("自动计算，导入时忽略")
    @ExcelProperty("金额")
    private String amount;
}
