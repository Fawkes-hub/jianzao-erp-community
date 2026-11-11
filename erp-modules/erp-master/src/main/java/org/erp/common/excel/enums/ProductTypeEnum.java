package org.erp.common.excel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 产品类型，用于 Excel 模板下拉。
 */
@Getter
@AllArgsConstructor
public enum ProductTypeEnum {

    PRODUCT("PRODUCT", "成品"),
    MATERIAL("MATERIAL", "原料");

    private final String code;
    private final String text;
}
