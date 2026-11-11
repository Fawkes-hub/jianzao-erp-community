package org.erp.common.excel;

/**
 * Excel 数据行及其原始行号。
 */
public record ExcelRow<T>(int rowNumber, T data) {
}
