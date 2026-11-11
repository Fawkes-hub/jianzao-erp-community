package org.erp.master.excel;

import org.erp.common.excel.ExcelRow;
import org.erp.common.excel.ImportResult;
import org.erp.master.domain.ErpWarehouse;
import org.erp.master.domain.bo.ErpWarehouseBo;
import org.erp.master.mapper.ErpWarehouseMapper;
import org.erp.master.service.IErpWarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 仓库导入核心逻辑单测。
 */
@Tag("dev")
class WarehouseExcelServiceTest {

    private IErpWarehouseService warehouseService;
    private ErpWarehouseMapper warehouseMapper;
    private WarehouseExcelService excelService;

    @BeforeEach
    void setUp() {
        warehouseService = mock(IErpWarehouseService.class);
        warehouseMapper = mock(ErpWarehouseMapper.class);
        excelService = new WarehouseExcelService(warehouseService, warehouseMapper);
    }

    @Test
    void blankCodeIsLeftToServiceForAutoGeneration() {
        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row(null, "主仓"))), false);

        assertEquals(1, result.getSuccess());
        ArgumentCaptor<ErpWarehouseBo> captor = ArgumentCaptor.forClass(ErpWarehouseBo.class);
        verify(warehouseService).insert(captor.capture());
        assertNull(captor.getValue().getCode());
        assertEquals("主仓", captor.getValue().getName());
    }

    @Test
    void validationErrorRejectsWholeBatch() {
        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("CK1", ""))), false);

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("仓库名称不能为空"));
        verifyNoInteractions(warehouseService);
    }

    @Test
    void softDeletedCodeIsRejected() {
        when(warehouseMapper.selectOne(any())).thenReturn(null);
        when(warehouseMapper.selectDeletedIdByCode("CK1")).thenReturn(7L);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("CK1", "主仓"))), false);

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("编码已被删除记录占用"));
        verifyNoInteractions(warehouseService);
    }

    @Test
    void existingCodeUpdatedWhenUpdateSupportEnabled() {
        ErpWarehouse existing = new ErpWarehouse();
        existing.setId(3L);
        existing.setCode("CK1");
        when(warehouseMapper.selectOne(any())).thenReturn(existing);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("CK1", "主仓"))), true);

        assertEquals(1, result.getSuccess());
        ArgumentCaptor<ErpWarehouseBo> captor = ArgumentCaptor.forClass(ErpWarehouseBo.class);
        verify(warehouseService).update(captor.capture());
        assertEquals(3L, captor.getValue().getId());
        verify(warehouseService, never()).insert(any());
    }

    private WarehouseImportRow row(String code, String name) {
        WarehouseImportRow row = new WarehouseImportRow();
        row.setCode(code);
        row.setName(name);
        return row;
    }
}
