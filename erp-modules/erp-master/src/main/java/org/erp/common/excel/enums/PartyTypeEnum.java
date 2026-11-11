package org.erp.common.excel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 往来类型，用于 Excel 模板下拉。
 */
@Getter
@AllArgsConstructor
public enum PartyTypeEnum {

    AR("AR", "应收"),
    AP("AP", "应付");

    private final String code;
    private final String text;
}
