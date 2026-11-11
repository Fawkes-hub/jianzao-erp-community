package org.erp.fin.excel;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.dromara.common.core.exception.ServiceException;
import org.erp.common.excel.ExcelRow;
import org.erp.common.excel.ImportResult;
import org.erp.fin.domain.bo.FinOpeningBo;
import org.erp.fin.domain.vo.FinOpeningVo;
import org.erp.fin.service.IFinOpeningService;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpSupplierMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 期初往来导入导出单元测试（DOC/22 §8）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("dev")
class FinOpeningExcelServiceTest {

    @Mock
    private IFinOpeningService openingService;
    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpSupplierMapper supplierMapper;

    private FinOpeningExcelService service;

    @BeforeEach
    void setUp() {
        service = new FinOpeningExcelService(openingService, customerMapper, supplierMapper);

        ErpCustomer customer = new ErpCustomer();
        customer.setId(9L);
        customer.setCode("C001");
        customer.setName("示例机械");
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(customer);

        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(10L);
        supplier.setCode("S001");
        supplier.setName("示例钢材");
        when(supplierMapper.selectOne(any(Wrapper.class))).thenReturn(supplier);

        when(openingService.save(any(FinOpeningBo.class))).thenReturn(new FinOpeningVo());
    }

    @Test
    void importRows_success_mapsPartyTypeAndCounts() {
        ImportResult result = service.importRows(rows(
            row("应收", "C001", "2000.00", "期初应收"),
            row("应付", "S001", "800.50", null)));

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getSuccess());
        assertEquals(0, result.getFailed());

        ArgumentCaptor<FinOpeningBo> captor = ArgumentCaptor.forClass(FinOpeningBo.class);
        verify(openingService, times(2)).save(captor.capture());
        assertEquals("AR", captor.getAllValues().get(0).getPartyType());
        assertEquals(Long.valueOf(9L), captor.getAllValues().get(0).getPartyId());
        assertEquals("AP", captor.getAllValues().get(1).getPartyType());
        assertEquals(Long.valueOf(10L), captor.getAllValues().get(1).getPartyId());
        assertEquals(0, new BigDecimal("800.50").compareTo(captor.getAllValues().get(1).getAmount()));
    }

    @Test
    void importRows_unknownCustomerCode_failsWithoutSaving() {
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        ImportResult result = service.importRows(rows(row("应收", "UNKNOWN", "1", null)));

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("客户编码不存在"));
        verify(openingService, never()).save(any(FinOpeningBo.class));
    }

    @Test
    void importRows_unknownSupplierCode_failsWithoutSaving() {
        when(supplierMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        ImportResult result = service.importRows(rows(row("应付", "UNKNOWN", "1", null)));

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("供应商编码不存在"));
        verify(openingService, never()).save(any(FinOpeningBo.class));
    }

    @Test
    void importRows_invalidPartyType_rejected() {
        ImportResult result = service.importRows(rows(row("其他", "C001", "1", null)));

        assertEquals(1, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("往来类型取值无效"));
        verify(openingService, never()).save(any(FinOpeningBo.class));
    }

    @Test
    void importRows_negativeOrTooManyDecimalsAmount_rejected() {
        ImportResult result = service.importRows(rows(
            row("应收", "C001", "-1", null),
            row("应收", "C001", "1.234", null)));

        assertEquals(2, result.getFailed());
        assertTrue(result.getErrors().get(0).message().contains("期初金额不能为负"));
        assertTrue(result.getErrors().get(1).message().contains("最多保留2位小数"));
        verify(openingService, never()).save(any(FinOpeningBo.class));
    }

    @Test
    void importRows_confirmedRow_rollsBackWholeBatch() {
        when(openingService.save(any(FinOpeningBo.class)))
            .thenThrow(new ServiceException("期初已确认锁定，请先反确认", 40902));

        ImportResult result = service.importRows(rows(
            row("应收", "C001", "100", null),
            row("应付", "S001", "200", null)));

        assertEquals(2, result.getTotal());
        assertEquals(0, result.getSuccess());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getSkipped());
        assertTrue(result.getErrors().get(0).message().contains("整批已回滚"));
        verify(openingService, times(1)).save(any(FinOpeningBo.class));
    }

    @Test
    void toExportRows_mapsPartyTypeAndCode() {
        FinOpeningVo ar = new FinOpeningVo();
        ar.setPartyType("AR");
        ar.setPartyId(9L);
        ar.setAmount(new BigDecimal("2000.00"));
        ar.setRemark("期初应收");

        FinOpeningVo ap = new FinOpeningVo();
        ap.setPartyType("AP");
        ap.setPartyId(10L);
        ap.setAmount(new BigDecimal("800.50"));

        ErpCustomer customer = new ErpCustomer();
        customer.setId(9L);
        customer.setCode("C001");
        when(customerMapper.selectById(9L)).thenReturn(customer);

        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(10L);
        supplier.setCode("S001");
        when(supplierMapper.selectById(10L)).thenReturn(supplier);

        List<FinOpeningExportVo> exportRows = service.toExportRows(List.of(ar, ap));

        assertEquals(2, exportRows.size());
        assertEquals("应收", exportRows.get(0).getPartyType());
        assertEquals("C001", exportRows.get(0).getPartyCode());
        assertEquals("应付", exportRows.get(1).getPartyType());
        assertEquals("S001", exportRows.get(1).getPartyCode());
    }

    // ---------- 辅助 ----------

    private FinOpeningImportRow row(String partyType, String partyCode, String amount, String remark) {
        FinOpeningImportRow row = new FinOpeningImportRow();
        row.setPartyType(partyType);
        row.setPartyCode(partyCode);
        row.setAmount(amount);
        row.setRemark(remark);
        return row;
    }

    private List<ExcelRow<FinOpeningImportRow>> rows(FinOpeningImportRow... data) {
        List<ExcelRow<FinOpeningImportRow>> rows = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            rows.add(new ExcelRow<>(i + 2, data[i]));
        }
        return rows;
    }
}
