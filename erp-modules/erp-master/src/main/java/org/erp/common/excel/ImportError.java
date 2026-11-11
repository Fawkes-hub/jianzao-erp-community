package org.erp.common.excel;

/**
 * 导入错误行。
 *
 * @param row     Excel 行号（从 1 开始，含表头）
 * @param message 错误说明
 */
public record ImportError(int row, String message) {
}
