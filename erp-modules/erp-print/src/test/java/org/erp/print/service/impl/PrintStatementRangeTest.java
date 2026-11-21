package org.erp.print.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.erp.fin.domain.FinArAp;
import org.erp.fin.domain.FinReceipt;
import org.erp.fin.mapper.FinArApMapper;
import org.erp.fin.mapper.FinReceiptMapper;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.print.domain.PrintTemplate;
import org.erp.print.service.IPrintTemplateService;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.mapper.ErpSaleItemMapper;
import org.erp.trade.mapper.ErpSaleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 对账单自定义区间测试：跨月区间、期初落在区间内、非法区间校验。
 */
@Tag("dev")
class PrintStatementRangeTest {

    @Test
    @DisplayName("跨月区间按起止日汇总，期初取开始日前余额")
    void rendersRangeAcrossMonths() {
        IPrintTemplateService templates = mock(IPrintTemplateService.class);
        ErpSaleMapper sales = mock(ErpSaleMapper.class);
        ErpSaleItemMapper items = mock(ErpSaleItemMapper.class);
        FinArApMapper ledger = mock(FinArApMapper.class);
        FinReceiptMapper receipts = mock(FinReceiptMapper.class);
        ErpCustomerMapper customers = mock(ErpCustomerMapper.class);

        // 区间 2026-09-01 ~ 2026-10-31：历史 7 月只有期初/销售/退货/收款
        FinArAp opening = arAp("OPENING", "50", "2026-07-01");
        FinArAp oldSale = arAp("SALE", "100", "2026-07-01");
        FinArAp oldReturn = arAp("SALE_RETURN", "-10", "2026-07-01");
        FinArAp rangeReturn = arAp("SALE_RETURN", "-20", "2026-09-15");
        FinArAp sepSale = arAp("SALE", "300", "2026-09-10");
        FinArAp octSale = arAp("SALE", "200", "2026-10-10");
        FinReceipt oldReceipt = receipt("30", "2026-07-01", null);
        FinReceipt octReceipt = receipt("40", "2026-10-10", "SK1");

        when(ledger.selectList(any())).thenReturn(
            List.of(opening, oldSale, oldReturn),
            List.of(rangeReturn),
            List.of(opening, oldSale, oldReturn, rangeReturn, sepSale, octSale));
        when(receipts.selectList(any())).thenReturn(
            List.of(oldReceipt),
            List.of(octReceipt),
            List.of(oldReceipt, octReceipt));
        when(sales.selectList(any())).thenReturn(List.of(
            sale("XS202609100001", "300", "2026-09-10"),
            sale("XS202610100001", "200", "2026-10-10")));
        when(items.selectList(any())).thenReturn(List.of());
        when(customers.selectById(9L)).thenReturn(new ErpCustomer());

        PrintServiceImpl service = new PrintServiceImpl(templates, sales, receipts, null, null, null,
            ledger, null, customers, null, null, items, null);
        String html = service.renderStatement(9L, null, "2026-09-01", "2026-10-31", null);

        assertTrue(html.contains("对账期间：<span>2026-09-01 至 2026-10-31</span>"), "应输出对账期间");
        // 期初 = 期初 50 + 历史销售 100 - 历史退货 10 - 历史收款 30 = 110
        assertTrue(html.contains("期初应收：<span>110.00</span>"), "期初应取区间开始日前余额");
        // 区间销售 = 300 + 200 - 退货 20 = 480；收款 40；未付 440
        assertTrue(html.contains("本期销售金额：<span>480.00</span>"));
        assertTrue(html.contains("本期收款金额：<span>40.00</span>"));
        assertTrue(html.contains("本期未付金额：<span>440.00</span>"));
        // 累计销售 = 历史 90 + 区间 480；累计收款 = 30 + 40；累计未付 = 620 - 70 = 550
        assertTrue(html.contains("累计销售金额：<span>570.00</span>"));
        assertTrue(html.contains("累计收款金额：<span>70.00</span>"));
        assertTrue(html.contains("累计未付金额：<b>550.00</b>"));
        assertTrue(html.contains("期末应收：<b>550.00</b>"));
    }

    @Test
    @DisplayName("期初截止日落在区间内时并入期初应收与期末应收")
    void openingInsideRange_countedInOpeningAndClosing() {
        IPrintTemplateService templates = mock(IPrintTemplateService.class);
        ErpSaleMapper sales = mock(ErpSaleMapper.class);
        ErpSaleItemMapper items = mock(ErpSaleItemMapper.class);
        FinArApMapper ledger = mock(FinArApMapper.class);
        FinReceiptMapper receipts = mock(FinReceiptMapper.class);
        ErpCustomerMapper customers = mock(ErpCustomerMapper.class);

        FinArAp opening = arAp("OPENING", "65004", "2026-09-30");
        when(ledger.selectList(any())).thenReturn(List.of(opening), List.of(), List.of(opening));
        when(receipts.selectList(any())).thenReturn(List.of(), List.of(), List.of());
        when(sales.selectList(any())).thenReturn(List.of());
        when(items.selectList(any())).thenReturn(List.of());
        when(customers.selectById(9L)).thenReturn(new ErpCustomer());

        PrintServiceImpl service = new PrintServiceImpl(templates, sales, receipts, null, null, null,
            ledger, null, customers, null, null, items, null);
        String html = service.renderStatement(9L, null, "2026-09-01", "2026-09-30", null);

        assertTrue(html.contains("期初应收：<span>65004.00</span>"), "区间内确认的期初应并入期初应收");
        assertTrue(html.contains("期末应收：<b>65004.00</b>"), "无业务发生时期末应等于期初");
    }

    @Test
    @DisplayName("month 与区间同时给出时区间优先")
    void rangeWinsOverMonth() {
        IPrintTemplateService templates = mock(IPrintTemplateService.class);
        PrintServiceImpl service = newService(templates);

        String html = service.renderStatement(9L, "2026-10", "2026-09-01", "2026-09-30", null);

        assertTrue(html.contains("对账期间：<span>2026-09-01 至 2026-09-30</span>"));
        assertFalse(html.contains("2026-10 月度"), "区间模式不应输出月度文案");
    }

    @Test
    @DisplayName("恰好 366 天允许、单日区间允许，且区间模式解析 STATEMENT_RANGE 模板")
    void acceptsBoundaryRanges() {
        IPrintTemplateService templates = mock(IPrintTemplateService.class);
        PrintServiceImpl service = newService(templates);

        // 2024 闰年整年含首尾 = 366 天
        String leapYear = service.renderStatement(9L, null, "2024-01-01", "2024-12-31", null);
        assertTrue(leapYear.contains("对账期间：<span>2024-01-01 至 2024-12-31</span>"));
        verify(templates).resolve(null, "STATEMENT_RANGE", 9L);

        String singleDay = service.renderStatement(9L, null, "2026-09-01", "2026-09-01", null);
        assertTrue(singleDay.contains("对账期间：<span>2026-09-01 至 2026-09-01</span>"));
    }

    @Test
    @DisplayName("非法区间直接拒绝：日期格式、起晚于止、跨度超限、只传一端、极端年份")
    void rejectsInvalidRange() {
        IPrintTemplateService templates = mock(IPrintTemplateService.class);
        PrintServiceImpl service = new PrintServiceImpl(templates, mock(ErpSaleMapper.class), mock(FinReceiptMapper.class),
            null, null, null, mock(FinArApMapper.class), null, mock(ErpCustomerMapper.class), null, null,
            mock(ErpSaleItemMapper.class), null);

        assertThrows(ServiceException.class, () -> service.renderStatement(9L, null, "2026-9-1", "2026-09-30", null));
        assertThrows(ServiceException.class, () -> service.renderStatement(9L, null, "2026-10-31", "2026-10-01", null));
        assertThrows(ServiceException.class, () -> service.renderStatement(9L, null, "2025-01-01", "2026-10-01", null));
        // 2024-01-01 ~ 2025-01-01 含首尾 367 天
        assertThrows(ServiceException.class, () -> service.renderStatement(9L, null, "2024-01-01", "2025-01-01", null));
        assertThrows(ServiceException.class, () -> service.renderStatement(9L, null, "2026-09-01", null, null));
        assertThrows(ServiceException.class, () -> service.renderStatement(9L, null, "+999999999-01-01", "+999999999-01-02", null));
    }

    @Test
    @DisplayName("显式 templateId 场景不匹配时拒绝渲染")
    void rejectsTemplateSceneMismatch() {
        IPrintTemplateService templates = mock(IPrintTemplateService.class);
        PrintTemplate wrongScene = new PrintTemplate();
        wrongScene.setScene("STATEMENT");
        when(templates.resolve(7L, "STATEMENT_RANGE", 9L)).thenReturn(wrongScene);
        PrintServiceImpl service = newService(templates);

        assertThrows(ServiceException.class, () -> service.renderStatement(9L, null, "2026-09-01", "2026-09-30", 7L));
    }

    private static PrintServiceImpl newService(IPrintTemplateService templates) {
        return new PrintServiceImpl(templates, mock(ErpSaleMapper.class), mock(FinReceiptMapper.class),
            null, null, null, mock(FinArApMapper.class), null, mock(ErpCustomerMapper.class), null, null,
            mock(ErpSaleItemMapper.class), null);
    }

    private static FinArAp arAp(String sourceType, String amount, String bizDate) {
        FinArAp row = new FinArAp();
        row.setSourceType(sourceType);
        row.setAmount(new BigDecimal(amount));
        row.setBizDate(java.sql.Date.valueOf(bizDate));
        return row;
    }

    private static FinReceipt receipt(String amount, String bizDate, String no) {
        FinReceipt row = new FinReceipt();
        row.setAmount(new BigDecimal(amount));
        row.setBizDate(java.sql.Date.valueOf(bizDate));
        row.setNegative(0);
        row.setNo(no);
        return row;
    }

    private static ErpSale sale(String no, String amount, String bizDate) {
        ErpSale row = new ErpSale();
        row.setId((long) no.hashCode());
        row.setNo(no);
        row.setBizDate(java.sql.Date.valueOf(bizDate));
        row.setTotalAmount(new BigDecimal(amount));
        return row;
    }
}
