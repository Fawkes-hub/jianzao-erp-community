package org.erp.stock.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;

import java.math.BigDecimal;

/**
 * 期初库存导出 VO（DOC/22 §4.1）。
 *
 * 不直接导出实体，避免泄漏 id/tenantId 等内部字段；名称列仅便于核对，导入时忽略。
 */
@Data
public class StOpeningExportVo {

    @ExcelRequired
    @ExcelNotation("必填，必须是产品资料中已存在的产品编码")
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelNotation("仅用于核对，导入时忽略")
    @ExcelProperty("产品名称")
    private String productName;

    @ExcelRequired
    @ExcelNotation("必填，必须是仓库资料中已存在的仓库编码")
    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @ExcelNotation("仅用于核对，导入时忽略")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelRequired
    @ExcelNotation(">=0，最多 3 位小数")
    @ExcelProperty("期初数量")
    private BigDecimal qty;

    @ExcelRequired
    @ExcelNotation(">=0，最多 4 位小数")
    @ExcelProperty("期初成本")
    private BigDecimal unitCost;

    @ExcelNotation("自动计算，导入时忽略")
    @ExcelProperty("金额")
    private BigDecimal amount;
}
