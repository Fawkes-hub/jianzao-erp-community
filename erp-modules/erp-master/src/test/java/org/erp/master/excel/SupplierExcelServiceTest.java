package org.erp.master.excel;

import org.erp.common.excel.ExcelRow;
import org.erp.common.excel.ImportResult;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.domain.bo.ErpSupplierBo;
import org.erp.master.mapper.ErpSupplierMapper;
import org.erp.master.service.IErpSupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 供应商导入核心逻辑单测。
 */
@Tag("dev")
class SupplierExcelServiceTest {

    private IErpSupplierService supplierService;
    private ErpSupplierMapper supplierMapper;
    private SupplierExcelService excelService;

    @BeforeEach
    void setUp() {
        supplierService = mock(IErpSupplierService.class);
        supplierMapper = mock(ErpSupplierMapper.class);
        excelService = new SupplierExcelService(supplierService, supplierMapper);
    }

    @Test
    void existingCodeSkippedWhenUpdateSupportDisabled() {
        ErpSupplier existing = new ErpSupplier();
        existing.setId(11L);
        existing.setCode("S001");
        when(supplierMapper.selectOne(any())).thenReturn(existing);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("S001", "钢材供应商"))), false);

        assertEquals(1, result.getSkipped());
        verify(supplierService, never()).insert(any());
        verify(supplierService, never()).update(any());
    }

    @Test
    void existingCodeUpdatedWhenUpdateSupportEnabled() {
        ErpSupplier existing = new ErpSupplier();
        existing.setId(11L);
        existing.setCode("S001");
        when(supplierMapper.selectOne(any())).thenReturn(existing);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("S001", "钢材供应商"))), true);

        assertEquals(1, result.getSuccess());
        ArgumentCaptor<ErpSupplierBo> captor = ArgumentCaptor.forClass(ErpSupplierBo.class);
        verify(supplierService).update(captor.capture());
        assertEquals(11L, captor.getValue().getId());
    }

    @Test
    void taxRateOutOfRangeRejected() {
        SupplierImportRow row = row("S001", "钢材供应商");
        row.setTaxRate("101");

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row)), false);

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("默认税率必须在0~100之间"));
        verifyNoInteractions(supplierService);
    }

    @Test
    void softDeletedCodeIsRejected() {
        when(supplierMapper.selectOne(any())).thenReturn(null);
        when(supplierMapper.selectDeletedIdByCode("S001")).thenReturn(12L);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("S001", "钢材供应商"))), false);

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("编码已被删除记录占用"));
        verifyNoInteractions(supplierService);
    }

    @Test
    void validTaxRateMappedToBo() {
        SupplierImportRow row = row("S001", "钢材供应商");
        row.setTaxRate("13");
        row.setStatus("停用");

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row)), false);

        assertEquals(1, result.getSuccess());
        ArgumentCaptor<ErpSupplierBo> captor = ArgumentCaptor.forClass(ErpSupplierBo.class);
        verify(supplierService).insert(captor.capture());
        assertEquals(0, new BigDecimal("13").compareTo(captor.getValue().getTaxRate()));
        assertEquals(0, captor.getValue().getStatus());
    }

    private SupplierImportRow row(String code, String name) {
        SupplierImportRow row = new SupplierImportRow();
        row.setCode(code);
        row.setName(name);
        return row;
    }
}
