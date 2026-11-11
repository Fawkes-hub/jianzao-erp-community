package org.erp.common.excel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 启用状态，用于 Excel 模板下拉。
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    ENABLED(1, "启用"),
    DISABLED(0, "停用");

    private final Integer code;
    private final String text;
}
