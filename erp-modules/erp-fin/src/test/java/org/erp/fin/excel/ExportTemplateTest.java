package org.erp.fin.excel;

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
 * 期初往来模板的枚举下拉、批注与必填红字。
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
    void partyTypeTemplateHasDropdownAndNotation() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(Collections.<FinOpeningExportVo>emptyList(), "期初往来",
            FinOpeningExportVo.class, output);
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()));
        try {
            Sheet sheet = workbook.getSheetAt(0);
            Cell partyType = findHeader(sheet, "往来类型");
            assertNotNull(partyType);
            assertNotNull(partyType.getCellComment(), "往来类型列应有批注");
            assertTrue(partyType.getCellComment().getString().getString().contains("应收"));
            assertTrue(sheet.getDataValidations().size() > 0, "往来类型应生成下拉");
            assertRequiredRed(workbook, partyType);
        } finally {
            workbook.close();
        }
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
