package org.erp.common.excel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发票类型，用于 Excel 模板下拉。
 */
@Getter
@AllArgsConstructor
public enum TaxTypeEnum {

    NO_TAX("NO_TAX", "不含税"),
    VAT_INVOICE("VAT_INVOICE", "增值税发票"),
    GENERAL_INVOICE("GENERAL_INVOICE", "普通发票");

    private final String code;
    private final String text;
}
