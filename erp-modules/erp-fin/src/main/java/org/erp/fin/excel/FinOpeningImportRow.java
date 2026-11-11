package org.erp.fin.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelEnumFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.erp.common.excel.enums.PartyTypeEnum;

/**
 * 期初往来导入行（DOC/22 §4.1）。
 *
 * <p>往来类型填「应收」或「应付」，往来单位编码对应客户/供应商编码；
 * 数值列按文本接收后统一校验，避免 Excel 浮点误差。模板下载复用本类，
 * 保证模板表头与导入解析列完全一致。</p>
 */
@Data
public class FinOpeningImportRow {

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
    private String amount;

    @ExcelNotation("可选，最多 500 字")
    @ExcelProperty("备注")
    private String remark;
}
