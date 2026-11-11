package org.erp.common.excel;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果。
 */
@Data
public class ImportResult {

    private int total;
    private int success;
    private int skipped;
    private int failed;
    private final List<ImportError> errors = new ArrayList<>();

    public void success() {
        total++;
        success++;
    }

    public void skipped() {
        total++;
        skipped++;
    }

    public void fail(int row, String message) {
        total++;
        failed++;
        errors.add(new ImportError(row, message));
    }

    public void failAll(int rowCount, String message) {
        for (int row = 2; row < rowCount + 2; row++) {
            fail(row, message);
        }
    }

    public String summary() {
        if (failed == 0) {
            return "导入完成：成功 " + success + " 行，跳过 " + skipped + " 行";
        }
        return "导入失败：共 " + total + " 行，成功 " + success + " 行，跳过 " + skipped
            + " 行，失败 " + failed + " 行";
    }
}
