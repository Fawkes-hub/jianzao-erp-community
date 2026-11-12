package org.erp.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.erp.fin.domain.FinCostLedger;
import org.erp.fin.mapper.FinCostLedgerMapper;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.domain.ErpSaleItem;
import org.erp.trade.domain.ErpSaleReturn;
import org.erp.trade.mapper.ErpSaleItemMapper;
import org.erp.trade.mapper.ErpSaleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * FinCostReportServiceImpl 单元测试（R20 口径）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("dev")
class FinCostReportServiceImplTest {

    @Mock
    private FinCostLedgerMapper costLedgerMapper;
    @Mock
    private ErpSaleMapper saleMapper;

    @Mock
    private org.erp.trade.mapper.ErpSaleReturnMapper saleReturnMapper;

    @Mock
    private ErpSaleItemMapper saleItemMapper;

    @Mock
    private org.erp.stock.service.IInventoryService inventoryService;

    private FinCostReportServiceImpl service;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus Lambda wrapper 需要实体 TableInfo 缓存（纯单测无 Spring 环境）
        initTableInfo(FinCostLedger.class);
        initTableInfo(ErpSale.class);
        initTableInfo(ErpSaleReturn.class);
        initTableInfo(ErpSaleItem.class);
        service = new FinCostReportServiceImpl(costLedgerMapper, saleMapper, saleReturnMapper, saleItemMapper, inventoryService);
        when(costLedgerMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(saleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(saleReturnMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(saleItemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(inventoryService.totalStockAmount()).thenReturn(BigDecimal.ZERO);
    }

    private static void initTableInfo(Class<?> clazz) {
        if (com.baomidou.mybatisplus.core.metadata.TableInfoHelper.getTableInfo(clazz) == null) {
            org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            assistant.setCurrentNamespace(clazz.getName());
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, clazz);
        }
    }

    @Test
    void report_sumsSalesCostAndTwoLedgerCostTypes_andSales() {
        // sumLedger 依次查 PRODUCTION_MATERIAL / OTHER_OUT / OTHER_EXPENSE，按调用序返回各 9000/600/300
        when(costLedgerMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(ledger("9000.00")), List.of(ledger("600.00")), List.of(ledger("300.00")));
        when(saleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                sale("20000.00", "9000.00"), sale("15000.00", "7000.00")
            ));

        var vo = service.report("2026-10");

        assertEquals("2026-10", vo.getMonth());
        assertEquals(0, new BigDecimal("35000.00").compareTo(vo.getSalesAmount()));
        assertEquals(0, new BigDecimal("16000.00").compareTo(vo.getSalesCost()));
        // 生产材料成本只作参考，不参与总成本
        assertEquals(0, new BigDecimal("9000.00").compareTo(vo.getProductionMaterialCost()));
        assertEquals(0, new BigDecimal("600.00").compareTo(vo.getOtherOutCost()));
        assertEquals(0, new BigDecimal("300.00").compareTo(vo.getOtherExpenseCost()));
        assertEquals(0, new BigDecimal("16900.00").compareTo(vo.getTotalCost()));
        assertEquals(0, new BigDecimal("18100.00").compareTo(vo.getGrossProfit()));
        // 毛利率 = 18100 / 35000 × 100 = 51.71
        assertEquals(0, new BigDecimal("51.71").compareTo(vo.getGrossMargin()));
    }

    @Test
    void report_生产材料成本不计入总成本() {
        when(costLedgerMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(ledger("5000.00")), List.of(), List.of());
        when(saleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sale("10000.00", "0")));

        var vo = service.report("2026-10");

        assertEquals(0, new BigDecimal("5000.00").compareTo(vo.getProductionMaterialCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalCost()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(vo.getGrossProfit()));
    }

    @Test
    void report_销售成本为空视为0() {
        when(saleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sale("5000.00", null)));

        var vo = service.report("2026-10");

        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getSalesCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalCost()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(vo.getGrossProfit()));
    }

    @Test
    void report_无销售时毛利率为null() {
        var vo = service.report("2026-09");

        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getSalesAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getGrossProfit()));
        // 销售收入为 0 → 毛利率不可计算（null，前端显示 '-'）
        assertNull(vo.getGrossMargin());
    }

    @Test
    void report_返回库存金额参考值() {
        when(inventoryService.totalStockAmount()).thenReturn(new BigDecimal("28.00"));

        var vo = service.report("2026-10");

        // 库存金额只作参考，不参与本期综合成本
        assertEquals(0, new BigDecimal("28.00").compareTo(vo.getStockAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalCost()));
    }

    @Test
    void report_统计本期成本缺失销售明细行数() {
        ErpSale sale = sale("10000.00", "0");
        sale.setId(5L);
        when(saleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sale));
        when(saleItemMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        var vo = service.report("2026-10");

        // 成本为 0 的销售明细行随报表返回，前端据此标「成本缺失」（DOC/26 §2.4 防线③）
        assertEquals(2, vo.getCostMissingCount().intValue());
    }

    @Test
    void report_无销售时成本缺失行数为0() {
        var vo = service.report("2026-10");

        assertEquals(0, vo.getCostMissingCount().intValue());
    }

    @Test
    void report_毛利率与销售单口径一致() {
        when(costLedgerMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(), List.of(), List.of());
        when(saleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sale("10000.00", "2500.00")));

        var vo = service.report("2026-10");

        // 毛利 7500，销售 10000 → 75.00
        assertEquals(0, new BigDecimal("7500.00").compareTo(vo.getGrossProfit()));
        assertEquals(0, new BigDecimal("75.00").compareTo(vo.getGrossMargin()));
    }

    @Test
    void report_毛利率保留两位小数HALF_UP() {
        when(costLedgerMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(), List.of(), List.of());
        when(saleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sale("999.99", "333.33")));

        var vo = service.report("2026-10");

        // (999.99 - 333.33) / 999.99 × 100 = 66.6667 → 66.67
        assertEquals(0, new BigDecimal("66.67").compareTo(vo.getGrossMargin()));
    }

    @Test
    void report_invalidMonth_rejected() {
        assertThrows(RuntimeException.class, () -> service.report("2026/10"));
        assertThrows(RuntimeException.class, () -> service.report("abc"));
    }

    @Test
    void report_blankMonth_defaultsToCurrentMonth() {
        var vo = service.report(null);
        assertEquals(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
            vo.getMonth());
    }

    // ---------- 辅助 ----------

    private FinCostLedger ledger(String amount) {
        FinCostLedger row = new FinCostLedger();
        row.setAmount(new BigDecimal(amount));
        return row;
    }

    private ErpSale sale(String totalAmount, String costAmount) {
        ErpSale s = new ErpSale();
        s.setTotalAmount(new BigDecimal(totalAmount));
        if (costAmount != null) {
            s.setCostAmount(new BigDecimal(costAmount));
        }
        return s;
    }
}
