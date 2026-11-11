package org.erp.common.excel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品数量精度（DOC/26 §10），用于 Excel 模板下拉。
 */
@Getter
@AllArgsConstructor
public enum QtyScaleEnum {

    INTEGER(0, "整数"),
    ONE(1, "1位"),
    TWO(2, "2位"),
    THREE(3, "3位");

    private final Integer code;
    private final String text;
}
