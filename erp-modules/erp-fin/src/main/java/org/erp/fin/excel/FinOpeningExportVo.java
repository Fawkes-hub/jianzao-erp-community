package org.erp.fin.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.erp.common.excel.enums.PartyTypeEnum;

import java.math.BigDecimal;

/**
 * 期初往来导出 VO（DOC/22 §4.1）。
 *
 * 往来类型导出为中文（应收/应付），导入时反向映射，不依赖 sys_dict。
 */
@Data
public class FinOpeningExportVo {

    @ExcelRequired
    @ExcelEnumFormat(enumClass = PartyTypeEnum.class)
    @ExcelNotation("可选：应收 / 应付")
    @ExcelProperty("往来类型")
    private String partyType;

    @ExcelRequired
    @ExcelNotation("应收填客户编码，应付填供应商编码；必须已存在")
    @ExcelProperty("往来单位编码")
    private String partyCode;

    @ExcelRequired
    @ExcelNotation(">=0，最多 2 位小数")
    @ExcelProperty("期初金额")
    private BigDecimal amount;

    @ExcelNotation("可选，最多 500 字")
    @ExcelProperty("备注")
    private String remark;
}
