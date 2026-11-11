package org.erp.common.excel;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取 Excel 数据行并保留行号，供逐行错误提示使用。
 */
public final class ExcelRowReader {

    private ExcelRowReader() {
    }

    public static <T> List<ExcelRow<T>> read(InputStream inputStream, Class<T> clazz) {
        List<ExcelRow<T>> rows = new ArrayList<>();
        FastExcel.read(inputStream, clazz, new AnalysisEventListener<T>() {
            @Override
            public void invoke(T data, AnalysisContext context) {
                rows.add(new ExcelRow<>(context.readRowHolder().getRowIndex() + 1, data));
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // no-op
            }
        }).sheet().doRead();
        return rows;
    }
}
