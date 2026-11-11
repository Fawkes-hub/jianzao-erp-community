package org.erp.fin.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.dromara.common.core.exception.ServiceException;
import org.erp.fin.domain.FinOpening;
import org.erp.fin.domain.bo.FinOpeningBo;
import org.erp.fin.mapper.FinOpeningMapper;
import org.erp.fin.service.IFinArApService;
import org.erp.master.domain.ErpCustomer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FinOpeningServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("dev")
class FinOpeningServiceImplTest {

    @Mock
    private FinOpeningMapper openingMapper;
    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpSupplierMapper supplierMapper;
    @Mock
    private IFinArApService arApService;

    private FinOpeningServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinOpeningServiceImpl(openingMapper, customerMapper, supplierMapper, arApService);
        when(openingMapper.insert(any(FinOpening.class))).thenReturn(1);
        when(openingMapper.updateById(any(FinOpening.class))).thenReturn(1);
        when(openingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    }

    @Test
    void confirm_createsOpeningLedger_andLocks() {
        FinOpening row = openingRow("DRAFT");
        when(openingMapper.selectById(1L)).thenReturn(row);

        service.confirm(1L);

        verify(arApService).createOpening(eq("AR"), eq(9L), eq(new BigDecimal("2000.00")), eq(1L), any());
        assertEquals("CONFIRMED", row.getStatus());
    }

    @Test
    void confirm_alreadyConfirmed_rejected40902() {
        when(openingMapper.selectById(1L)).thenReturn(openingRow("CONFIRMED"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.confirm(1L));
        assertEquals(40902, ex.getCode());
    }

    @Test
    void unconfirm_voidsOpeningLedger_backToDraft() {
        FinOpening row = openingRow("CONFIRMED");
        when(openingMapper.selectById(1L)).thenReturn(row);

        service.unconfirm(1L);

        verify(arApService).voidBySource("OPENING", 1L);
        assertEquals("DRAFT", row.getStatus());
    }

    @Test
    void unconfirm_ledgerSettled_propagates40904() {
        FinOpening row = openingRow("CONFIRMED");
        when(openingMapper.selectById(1L)).thenReturn(row);
        org.mockito.Mockito.doThrow(new ServiceException("已核销的应收应付需先反核销", 40904))
            .when(arApService).voidBySource("OPENING", 1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.unconfirm(1L));
        assertEquals(40904, ex.getCode());
    }

    @Test
    void save_confirmedRow_rejected40902() {
        FinOpening exist = new FinOpening();
        exist.setStatus("CONFIRMED");
        when(openingMapper.selectOne(any(Wrapper.class))).thenReturn(exist);

        ServiceException ex = assertThrows(ServiceException.class, () ->
            service.save(bo("2000")));
        assertEquals(40902, ex.getCode());
    }

    @Test
    void listAll_resolvesCustomerName() {
        FinOpening row = openingRow("DRAFT");
        when(openingMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(row));
        ErpCustomer c = new ErpCustomer();
        c.setId(9L);
        c.setName("示例机械");
        when(customerMapper.selectById(9L)).thenReturn(c);

        var list = service.listAll("AR");

        assertEquals(1, list.size());
        assertEquals("示例机械", list.get(0).getPartyName());
    }

    // ---------- 辅助 ----------

    private FinOpeningBo bo(String amount) {
        FinOpeningBo bo = new FinOpeningBo();
        bo.setPartyType("AR");
        bo.setPartyId(9L);
        bo.setAmount(new BigDecimal(amount));
        return bo;
    }

    private FinOpening openingRow(String status) {
        FinOpening row = new FinOpening();
        row.setId(1L);
        row.setPartyType("AR");
        row.setPartyId(9L);
        row.setAmount(new BigDecimal("2000.00"));
        row.setStatus(status);
        return row;
    }
}
