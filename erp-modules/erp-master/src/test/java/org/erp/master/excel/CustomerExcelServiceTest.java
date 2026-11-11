package org.erp.master.excel;

import org.erp.common.excel.ExcelRow;
import org.erp.common.excel.ImportResult;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.bo.ErpCustomerBo;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.service.IErpCustomerService;
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
 * 客户导入导出核心逻辑单测。
 */
@Tag("dev")
class CustomerExcelServiceTest {

    private IErpCustomerService customerService;
    private ErpCustomerMapper customerMapper;
    private CustomerExcelService excelService;

    @BeforeEach
    void setUp() {
        customerService = mock(IErpCustomerService.class);
        customerMapper = mock(ErpCustomerMapper.class);
        excelService = new CustomerExcelService(customerService, customerMapper);
    }

    @Test
    void validationErrorRejectsWholeBatchWithoutWrites() {
        CustomerImportRow row = row(null, "");

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row)), false);

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getSuccess());
        assertEquals(1, result.getFailed());
        assertEquals(2, result.getErrors().get(0).row());
        assertTrue(result.getErrors().get(0).message().contains("客户名称不能为空"));
        verifyNoInteractions(customerService);
    }

    @Test
    void existingCodeSkippedWhenUpdateSupportDisabled() {
        ErpCustomer existing = existing(9L, "C001");
        when(customerMapper.selectOne(any())).thenReturn(existing);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("C001", "远达机械"))), false);

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getSuccess());
        assertEquals(1, result.getSkipped());
        assertEquals(0, result.getFailed());
        verify(customerService, never()).insert(any());
        verify(customerService, never()).update(any());
    }

    @Test
    void existingCodeUpdatedWhenUpdateSupportEnabled() {
        ErpCustomer existing = existing(9L, "C001");
        when(customerMapper.selectOne(any())).thenReturn(existing);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("C001", "远达机械"))), true);

        assertEquals(1, result.getSuccess());
        ArgumentCaptor<ErpCustomerBo> captor = ArgumentCaptor.forClass(ErpCustomerBo.class);
        verify(customerService).update(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals("C001", captor.getValue().getCode());
        verify(customerService, never()).insert(any());
    }

    @Test
    void softDeletedCodeIsRejected() {
        when(customerMapper.selectOne(any())).thenReturn(null);
        when(customerMapper.selectDeletedIdByCode("C001")).thenReturn(5L);

        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row("C001", "远达机械"))), false);

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("编码已被删除记录占用"));
        verifyNoInteractions(customerService);
    }

    @Test
    void blankCodeIsLeftToServiceForAutoGeneration() {
        ImportResult result = excelService.importRows(List.of(new ExcelRow<>(2, row(null, "远达机械"))), false);

        assertEquals(1, result.getSuccess());
        verify(customerMapper, never()).selectOne(any());
        verify(customerMapper, never()).selectDeletedIdByCode(any());
        ArgumentCaptor<ErpCustomerBo> captor = ArgumentCaptor.forClass(ErpCustomerBo.class);
        verify(customerService).insert(captor.capture());
        assertNull(captor.getValue().getCode());
        assertEquals("远达机械", captor.getValue().getName());
    }

    @Test
    void successAndSkipCountsIncludeAllRows() {
        ErpCustomer existing = existing(9L, "B");
        when(customerMapper.selectOne(any())).thenReturn(null, existing);

        ImportResult result = excelService.importRows(List.of(
            new ExcelRow<>(2, row("A", "新增客户")),
            new ExcelRow<>(3, row("B", "已存在客户"))
        ), false);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccess());
        assertEquals(1, result.getSkipped());
        assertEquals(0, result.getFailed());
        verify(customerService).insert(any());
        verify(customerService, never()).update(any());
    }

    @Test
    void duplicateCodeInFileRejectsWholeBatch() {
        when(customerMapper.selectOne(any())).thenReturn(null);

        ImportResult result = excelService.importRows(List.of(
            new ExcelRow<>(2, row("DUP", "客户一")),
            new ExcelRow<>(3, row("DUP", "客户二"))
        ), false);

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("文件内客户编码重复"));
        verifyNoInteractions(customerService);
    }

    @Test
    void exportRowsMapStatusToChinese() {
        org.erp.master.domain.vo.ErpCustomerVo vo = new org.erp.master.domain.vo.ErpCustomerVo();
        vo.setCode("C001");
        vo.setName("远达机械");
        vo.setStatus(0);
        when(customerService.selectPage(any(), any(), any()))
            .thenReturn(org.dromara.common.mybatis.core.page.TableDataInfo.build(List.of(vo)));

        List<CustomerExportVo> rows = excelService.exportRows(null, null);

        assertEquals(1, rows.size());
        assertEquals("停用", rows.get(0).getStatus());
        assertEquals("C001", rows.get(0).getCode());
    }

    private CustomerImportRow row(String code, String name) {
        CustomerImportRow row = new CustomerImportRow();
        row.setCode(code);
        row.setName(name);
        return row;
    }

    private ErpCustomer existing(Long id, String code) {
        ErpCustomer customer = new ErpCustomer();
        customer.setId(id);
        customer.setCode(code);
        return customer;
    }
}
