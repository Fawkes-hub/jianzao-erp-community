package org.erp.master.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.excel.utils.ExcelUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 模板提示：必填列红字、表头批注、枚举下拉。
 */
@Tag("dev")
class ExportTemplateTest {

    private static GenericApplicationContext context;

    @BeforeAll
    static void setUpContext() {
        context = new GenericApplicationContext();
        context.registerBean(DictService.class, () -> mock(DictService.class));
        context.refresh();
        new SpringUtils().setApplicationContext(context);
    }

    @AfterAll
    static void tearDownContext() {
        new SpringUtils().setApplicationContext(null);
        context.close();
    }

    @Test
    void customerTemplateHasStatusDropdownAndNotation() throws Exception {
        Workbook workbook = generate(CustomerExportVo.class, "客户资料");
        try {
            Sheet sheet = workbook.getSheetAt(0);
            Cell status = findHeader(sheet, "状态");
            assertNotNull(status);
            assertNotNull(status.getCellComment(), "状态列应有批注");
            assertTrue(status.getCellComment().getString().getString().contains("启用"));

            Cell code = findHeader(sheet, "客户编码");
            assertNotNull(code.getCellComment(), "客户编码列应有批注");
            assertTrue(code.getCellComment().getString().getString().contains("自动生成"));

            assertTrue(sheet.getDataValidations().size() > 0, "应生成枚举下拉");
            assertRequiredRed(workbook, findHeader(sheet, "客户名称"));
        } finally {
            workbook.close();
        }
    }

    @Test
    void productTemplateHasTypeTaxTypeAndStatusDropdowns() throws Exception {
        Workbook workbook = generate(ProductExportVo.class, "产品资料");
        try {
            Sheet sheet = workbook.getSheetAt(0);
            assertTrue(sheet.getDataValidations().size() >= 3, "产品模板应有类型/发票类型/状态下拉");
            assertRequiredRed(workbook, findHeader(sheet, "产品名称"));
            assertRequiredRed(workbook, findHeader(sheet, "单位"));
            assertRequiredRed(workbook, findHeader(sheet, "类型"));
        } finally {
            workbook.close();
        }
    }

    private <T> Workbook generate(Class<T> clazz, String sheetName) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(Collections.<T>emptyList(), sheetName, clazz, output);
        return new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()));
    }

    private Cell findHeader(Sheet sheet, String text) {
        for (Cell cell : sheet.getRow(0)) {
            if (text.equals(cell.getStringCellValue())) {
                return cell;
            }
        }
        return null;
    }

    private void assertRequiredRed(Workbook workbook, Cell cell) {
        assertNotNull(cell);
        CellStyle style = cell.getCellStyle();
        Font font = workbook.getFontAt(style.getFontIndexAsInt());
        assertEquals(IndexedColors.RED.getIndex(), font.getColor(), "必填列表头应为红色");
    }
}
