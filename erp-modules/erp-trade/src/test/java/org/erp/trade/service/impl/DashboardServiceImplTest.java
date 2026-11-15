package org.erp.trade.service.impl;

import org.erp.fin.domain.FinArAp;
import org.erp.fin.domain.FinCostLedger;
import org.erp.fin.domain.FinPayment;
import org.erp.fin.domain.FinReceipt;
import org.erp.fin.mapper.FinArApMapper;
import org.erp.fin.mapper.FinCostLedgerMapper;
import org.erp.fin.mapper.FinPaymentMapper;
import org.erp.fin.mapper.FinReceiptMapper;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpProduct;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpProductMapper;
import org.erp.master.mapper.ErpSupplierMapper;
import org.erp.stock.domain.ErpStock;
import org.erp.stock.mapper.ErpStockMapper;
import org.erp.trade.domain.ErpPurchase;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.domain.vo.DashboardPartnerMonthVo;
import org.erp.trade.domain.vo.DashboardPartnerRankVo;
import org.erp.trade.domain.vo.DashboardSummaryVo;
import org.erp.trade.domain.vo.DashboardTrendVo;
import org.erp.trade.mapper.ErpPurchaseMapper;
import org.erp.trade.mapper.ErpSaleMapper;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 工作台看板单测：口径正确性（VOID/REVERSED 排除、成品原料过滤、状态过滤）、
 * 最近单据排序截断、趋势月序列与参数校验。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceImplTest {

    @Mock
    private ErpSaleMapper saleMapper;
    @Mock
    private ErpPurchaseMapper purchaseMapper;
    @Mock
    private FinArApMapper arApMapper;
    @Mock
    private FinReceiptMapper receiptMapper;
    @Mock
    private FinPaymentMapper paymentMapper;
    @Mock
    private FinCostLedgerMapper costLedgerMapper;
    @Mock
    private ErpStockMapper stockMapper;

    @Mock
    private org.erp.stock.mapper.ErpStockAlertMapper stockAlertMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpSupplierMapper supplierMapper;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus Lambda wrapper 需要实体 TableInfo 缓存（纯单测无 Spring 环境）
        initTableInfo(ErpSale.class);
        initTableInfo(ErpPurchase.class);
        initTableInfo(FinArAp.class);
        initTableInfo(FinReceipt.class);
        initTableInfo(FinPayment.class);
        initTableInfo(FinCostLedger.class);
        initTableInfo(ErpStock.class);
        initTableInfo(ErpProduct.class);
        service = new DashboardServiceImpl(saleMapper, purchaseMapper,
            arApMapper, receiptMapper, paymentMapper, costLedgerMapper, stockMapper, stockAlertMapper, productMapper,
            customerMapper, supplierMapper);
        lenient().when(saleMapper.selectList(any())).thenReturn(List.of());
        lenient().when(purchaseMapper.selectList(any())).thenReturn(List.of());
        lenient().when(arApMapper.selectList(any())).thenReturn(List.of());
        lenient().when(receiptMapper.selectList(any())).thenReturn(List.of());
        lenient().when(paymentMapper.selectList(any())).thenReturn(List.of());
        lenient().when(costLedgerMapper.selectList(any())).thenReturn(List.of());
        lenient().when(stockMapper.selectList(any())).thenReturn(List.of());
        lenient().when(productMapper.selectByIds(any())).thenReturn(List.of());
        lenient().when(saleMapper.selectCount(any())).thenReturn(0L);
        lenient().when(purchaseMapper.selectCount(any())).thenReturn(0L);
        lenient().when(receiptMapper.selectCount(any())).thenReturn(0L);
        lenient().when(paymentMapper.selectCount(any())).thenReturn(0L);
    }

    private Date date(int y, int m, int d) {
        return Date.from(LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant());
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

    private ErpSale sale(String no, Date bizDate, String total, String gross) {
        ErpSale s = new ErpSale();
        s.setNo(no);
        s.setBizDate(bizDate);
        s.setTotalAmount(new BigDecimal(total));
        s.setGrossProfit(new BigDecimal(gross));
        return s;
    }

    @Test
    @DisplayName("空数据：所有指标为 0")
    void emptyData() {
        DashboardSummaryVo vo = service.summary();
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getMonthSales()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getUnreceived()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getFinishedStockQty()));
        assertEquals(0L, vo.getDraftTodos().total());
        assertTrue(vo.getRecentDocs().isEmpty());
    }

    @Test
    @DisplayName("未收/未付：VOID 台账排除，settled 正确扣减")
    void unsettledExcludesVoid() {
        FinArAp arOpen = new FinArAp();
        arOpen.setPartyType("AR");
        arOpen.setAmount(new BigDecimal("1000"));
        arOpen.setSettledAmount(new BigDecimal("400"));
        arOpen.setStatus("PARTIAL");
        FinArAp arVoid = new FinArAp();
        arVoid.setPartyType("AR");
        arVoid.setAmount(new BigDecimal("999"));
        arVoid.setSettledAmount(BigDecimal.ZERO);
        arVoid.setStatus("VOID");
        FinArAp apOpen = new FinArAp();
        apOpen.setPartyType("AP");
        apOpen.setAmount(new BigDecimal("500"));
        apOpen.setSettledAmount(new BigDecimal("200"));
        apOpen.setStatus("OPEN");
        when(arApMapper.selectList(any())).thenReturn(List.of(arOpen, arVoid, apOpen));

        DashboardSummaryVo vo = service.summary();
        assertEquals(0, new BigDecimal("600.00").compareTo(vo.getUnreceived()));
        assertEquals(0, new BigDecimal("300.00").compareTo(vo.getUnpaid()));
    }

    @Test
    @DisplayName("库存：成品/原料按 product type 划分，金额=Σqty×avg_cost")
    void stockSplitByProductType() {
        ErpStock s1 = new ErpStock();
        s1.setProductId(1L);
        s1.setQty(new BigDecimal("10"));
        s1.setAvgCost(new BigDecimal("26.417"));
        ErpStock s2 = new ErpStock();
        s2.setProductId(2L);
        s2.setQty(new BigDecimal("125"));
        s2.setAvgCost(new BigDecimal("10.5667"));
        when(stockMapper.selectList(any())).thenReturn(List.of(s1, s2));
        ErpProduct p1 = new ErpProduct();
        p1.setId(1L);
        p1.setType("PRODUCT");
        ErpProduct p2 = new ErpProduct();
        p2.setId(2L);
        p2.setType("MATERIAL");
        when(productMapper.selectByIds(any())).thenReturn(List.of(p1, p2));

        DashboardSummaryVo vo = service.summary();
        assertEquals(0, new BigDecimal("10").compareTo(vo.getFinishedStockQty()));
        assertEquals(0, new BigDecimal("264.17").compareTo(vo.getFinishedStockAmount()));
        assertEquals(0, new BigDecimal("125").compareTo(vo.getMaterialStockQty()));
    }

    @Test
    @DisplayName("库存：产品字典缺失的类型不计入")
    void stockSkipsUnknownProduct() {
        ErpStock s = new ErpStock();
        s.setProductId(99L);
        s.setQty(new BigDecimal("10"));
        s.setAvgCost(BigDecimal.ONE);
        when(stockMapper.selectList(any())).thenReturn(List.of(s));
        when(productMapper.selectByIds(any())).thenReturn(List.of());

        DashboardSummaryVo vo = service.summary();
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getFinishedStockQty()));
    }

    @Test
    @DisplayName("最近单据：混排按 bizDate 倒序，截断 10 条")
    void recentDocsSortedAndTruncated() {
        when(saleMapper.selectList(any())).thenReturn(List.of(
            sale("XS001", date(2026, 10, 1), "100", "10"),
            sale("XS002", date(2026, 10, 3), "200", "20")));
        ErpPurchase p = new ErpPurchase();
        p.setNo("CG001");
        p.setBizDate(date(2026, 10, 2));
        p.setTotalAmount(new BigDecimal("50"));
        when(purchaseMapper.selectList(any())).thenReturn(List.of(p));

        DashboardSummaryVo vo = service.summary();
        assertEquals(3, vo.getRecentDocs().size());
        assertEquals("XS002", vo.getRecentDocs().get(0).getNo());
        assertEquals("CG001", vo.getRecentDocs().get(1).getNo());
        assertEquals("XS001", vo.getRecentDocs().get(2).getNo());
        assertEquals("SALE", vo.getRecentDocs().get(0).getType());
        assertEquals(0, new BigDecimal("200").compareTo(vo.getRecentDocs().get(0).getAmount()));
    }

    @Test
    @DisplayName("最近单据：bizDate 为空排在末尾不抛 NPE")
    void recentDocsNullDateLast() {
        when(saleMapper.selectList(any())).thenReturn(List.of(sale("XS001", null, "100", "10")));
        ErpPurchase p = new ErpPurchase();
        p.setNo("CG001");
        p.setBizDate(date(2026, 10, 2));
        when(purchaseMapper.selectList(any())).thenReturn(List.of(p));

        DashboardSummaryVo vo = service.summary();
        assertEquals(2, vo.getRecentDocs().size());
        assertEquals("CG001", vo.getRecentDocs().get(0).getNo());
    }

    @Test
    @DisplayName("草稿待办：四类合计")
    void draftTodosTotal() {
        when(saleMapper.selectCount(any())).thenReturn(2L);
        when(purchaseMapper.selectCount(any())).thenReturn(1L);
        when(receiptMapper.selectCount(any())).thenReturn(0L);
        when(paymentMapper.selectCount(any())).thenReturn(1L);

        DashboardSummaryVo vo = service.summary();
        assertEquals(4L, vo.getDraftTodos().total());
        assertEquals(2L, vo.getDraftTodos().getSaleDrafts());
    }

    @Test
    @DisplayName("趋势：默认 6 个月，月份序列从早到晚，profit=sales-cost")
    void trendDefaultSixMonths() {
        DashboardTrendVo vo = service.trend(null);
        assertEquals(6, vo.getMonths().size());
        assertEquals(6, vo.getSales().size());
        LocalDate expectedLast = LocalDate.now().withDayOfMonth(1);
        assertEquals(expectedLast.minusMonths(5).toString().substring(0, 7), vo.getMonths().get(0));
        assertEquals(expectedLast.toString().substring(0, 7), vo.getMonths().get(5));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getProfit().get(0)));
    }

    @Test
    @DisplayName("趋势：12 个月合法")
    void trendTwelveMonths() {
        DashboardTrendVo vo = service.trend(12);
        assertEquals(12, vo.getMonths().size());
    }

    @Test
    @DisplayName("趋势：0/13 拒绝")
    void trendInvalidMonths() {
        assertThrows(ServiceException.class, () -> service.trend(0));
        assertThrows(ServiceException.class, () -> service.trend(13));
        assertThrows(ServiceException.class, () -> service.trend(-1));
    }

    @Test
    @DisplayName("成本趋势：其他出库/其他支出台账按月分组（聚合只做月分组，ACTIVE 由查询条件保证）")
    void trendCostGroupsByMonth() {
        FinCostLedger c1 = new FinCostLedger();
        c1.setAmount(new BigDecimal("317.59"));
        c1.setBizDate(currentMonthDate(5));
        when(costLedgerMapper.selectList(any())).thenReturn(List.of(c1));

        DashboardTrendVo vo = service.trend(1);
        assertEquals(0, new BigDecimal("317.59").compareTo(vo.getCost().get(0)));
    }

    @Test
    @DisplayName("成本趋势 = 销售出库成本 + 其他出库/其他支出，不含生产材料成本")
    void trendCostUsesSalesCostBasis() {
        ErpSale s = new ErpSale();
        s.setStatus("CONFIRMED");
        s.setBizDate(currentMonthDate(5));
        s.setTotalAmount(new BigDecimal("1000"));
        s.setCostAmount(new BigDecimal("700"));
        when(saleMapper.selectList(any())).thenReturn(List.of(s));

        FinCostLedger otherOut = new FinCostLedger();
        otherOut.setAmount(new BigDecimal("50"));
        otherOut.setBizDate(currentMonthDate(6));
        when(costLedgerMapper.selectList(any())).thenReturn(List.of(otherOut));

        DashboardTrendVo vo = service.trend(1);
        assertEquals(0, new BigDecimal("750.00").compareTo(vo.getCost().get(0)));
        assertEquals(0, new BigDecimal("250.00").compareTo(vo.getProfit().get(0)));
    }

    @Test
    @DisplayName("收款金额 null 视为 0")
    void receiptNullAmountTreatedAsZero() {
        FinReceipt r = new FinReceipt();
        r.setAmount(null);
        r.setBizDate(date(2026, 10, 1));
        when(receiptMapper.selectList(any())).thenReturn(List.of(r));

        DashboardSummaryVo vo = service.summary();
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getMonthReceiptAmount()));
    }

    @Test
    @DisplayName("付款单草稿计数进入待办")
    void paymentDraftTodo() {
        when(paymentMapper.selectCount(any())).thenReturn(4L);
        DashboardSummaryVo vo = service.summary();
        assertEquals(4L, vo.getDraftTodos().getPaymentDrafts());
    }

    @Test
    @DisplayName("本月销售毛利率 = Σgross_profit / Σtotal_amount × 100")
    void monthGrossMargin() {
        // 第一次 selectList 取销售额，第二次取毛利
        when(saleMapper.selectList(any())).thenReturn(
            List.of(sale("XS001", date(2026, 10, 1), "10000", "0")),
            List.of(sale("XS001", date(2026, 10, 1), "0", "2500"))
        );

        DashboardSummaryVo vo = service.summary();
        assertEquals(0, new BigDecimal("10000.00").compareTo(vo.getMonthSales()));
        assertEquals(0, new BigDecimal("2500.00").compareTo(vo.getMonthGrossProfit()));
        assertEquals(0, new BigDecimal("25.00").compareTo(vo.getMonthGrossMargin()));
    }

    @Test
    @DisplayName("本月无销售时毛利率为 null")
    void monthGrossMarginNullWhenNoSales() {
        DashboardSummaryVo vo = service.summary();
        assertNull(vo.getMonthGrossMargin());
    }

    /* ------------------------------ 往来单位当月明细 ------------------------------ */

    /** 当前月份内的某一天，保证落在服务端统计窗口内 */
    private Date currentMonthDate(int day) {
        LocalDate now = LocalDate.now().withDayOfMonth(1);
        return Date.from(now.plusDays(day - 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("往来单位当月明细：客户口径统计本月销售与收款，并列出单据")
    void partnerMonthCustomer() {
        ErpCustomer customer = new ErpCustomer();
        customer.setId(7L);
        customer.setName("宏达建材");
        when(customerMapper.selectById(7L)).thenReturn(customer);

        ErpSale sale = new ErpSale();
        sale.setNo("XS20261101001");
        sale.setBizDate(currentMonthDate(5));
        sale.setTotalAmount(new BigDecimal("1000"));
        sale.setGrossProfit(new BigDecimal("300"));
        when(saleMapper.selectList(any())).thenReturn(List.of(sale));

        FinReceipt receipt = new FinReceipt();
        receipt.setNo("SK20261101001");
        receipt.setBizDate(currentMonthDate(8));
        receipt.setAmount(new BigDecimal("400"));
        when(receiptMapper.selectList(any())).thenReturn(List.of(receipt));

        DashboardPartnerMonthVo vo = service.partnerMonth("CUSTOMER", 7L);
        assertEquals("宏达建材", vo.getPartyName());
        assertEquals(1L, vo.getDocCount());
        assertEquals(0, new BigDecimal("1000.00").compareTo(vo.getSalesAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(vo.getGrossProfit()));
        assertEquals(0, new BigDecimal("30.00").compareTo(vo.getGrossMargin()));
        assertEquals(0, new BigDecimal("400.00").compareTo(vo.getReceiptAmount()));
        assertEquals(2, vo.getDocs().size());
        // 收款单业务日期晚于销售单，倒序排列时收款在前
        assertEquals("RECEIPT", vo.getDocs().get(0).getType());
        assertEquals("SALE", vo.getDocs().get(1).getType());
    }

    @Test
    @DisplayName("往来单位当月明细：供货商口径统计本月采购与付款")
    void partnerMonthSupplier() {
        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(9L);
        supplier.setName("钢材供应商");
        when(supplierMapper.selectById(9L)).thenReturn(supplier);

        ErpPurchase purchase = new ErpPurchase();
        purchase.setNo("CG20261101001");
        purchase.setBizDate(currentMonthDate(3));
        purchase.setTotalAmount(new BigDecimal("800"));
        when(purchaseMapper.selectList(any())).thenReturn(List.of(purchase));

        FinPayment payment = new FinPayment();
        payment.setNo("FK20261101001");
        payment.setBizDate(currentMonthDate(9));
        payment.setAmount(new BigDecimal("300"));
        when(paymentMapper.selectList(any())).thenReturn(List.of(payment));

        DashboardPartnerMonthVo vo = service.partnerMonth("SUPPLIER", 9L);
        assertEquals("钢材供应商", vo.getPartyName());
        assertEquals(1L, vo.getDocCount());
        assertEquals(0, new BigDecimal("800.00").compareTo(vo.getPurchaseAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(vo.getPaymentAmount()));
        assertEquals(2, vo.getDocs().size());
        assertEquals("PAYMENT", vo.getDocs().get(0).getType());
        assertEquals("PURCHASE", vo.getDocs().get(1).getType());
    }

    @Test
    @DisplayName("往来单位当月明细：客户无销售时毛利率为 null，单据列表为空")
    void partnerMonthCustomerNoData() {
        DashboardPartnerMonthVo vo = service.partnerMonth("CUSTOMER", 7L);
        assertNull(vo.getPartyName());
        assertEquals(0L, vo.getDocCount());
        assertNull(vo.getGrossMargin());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getSalesAmount()));
        assertTrue(vo.getDocs().isEmpty());
    }

    @Test
    @DisplayName("往来单位当月明细：非法单位类型或缺失单位 ID 抛参数异常")
    void partnerMonthInvalidParams() {
        assertThrows(ServiceException.class, () -> service.partnerMonth("CUSTOMER", null));
        assertThrows(ServiceException.class, () -> service.partnerMonth("OTHER", 7L));
    }

    /* ------------------------------ 往来单位本月排行 ------------------------------ */

    @Test
    @DisplayName("排行：客户按本月销售额倒序，含毛利与收款")
    void partnerRankCustomerSortedByAmount() {
        ErpCustomer c1 = new ErpCustomer();
        c1.setId(7L);
        c1.setCode("KH01");
        c1.setName("远达机械制造");
        ErpCustomer c2 = new ErpCustomer();
        c2.setId(8L);
        c2.setCode("KH02");
        c2.setName("恒通机电设备");
        when(customerMapper.selectByIds(any())).thenReturn(List.of(c1, c2));

        ErpSale big = new ErpSale();
        big.setCustomerId(7L);
        big.setTotalAmount(new BigDecimal("1000"));
        big.setGrossProfit(new BigDecimal("300"));
        ErpSale small = new ErpSale();
        small.setCustomerId(8L);
        small.setTotalAmount(new BigDecimal("400"));
        small.setGrossProfit(new BigDecimal("100"));
        when(saleMapper.selectList(any())).thenReturn(List.of(big, small));

        FinReceipt receipt = new FinReceipt();
        receipt.setCustomerId(7L);
        receipt.setAmount(new BigDecimal("500"));
        when(receiptMapper.selectList(any())).thenReturn(List.of(receipt));

        DashboardPartnerRankVo vo = service.partnerRank("CUSTOMER");
        assertEquals(2, vo.getItems().size());
        DashboardPartnerRankVo.RankItem first = vo.getItems().get(0);
        assertEquals(7L, first.getPartyId());
        assertEquals("远达机械制造", first.getPartyName());
        assertEquals("KH01", first.getPartyCode());
        assertEquals(0, new BigDecimal("1000.00").compareTo(first.getAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(first.getGrossProfit()));
        assertEquals(0, new BigDecimal("30.00").compareTo(first.getGrossMargin()));
        assertEquals(0, new BigDecimal("500.00").compareTo(first.getReceiptAmount()));
        assertEquals(1L, first.getDocCount());
        assertEquals(8L, vo.getItems().get(1).getPartyId());
    }

    @Test
    @DisplayName("排行：同一客户多张销售单金额累加，单据数累加")
    void partnerRankAggregatesSameParty() {
        ErpCustomer c1 = new ErpCustomer();
        c1.setId(7L);
        c1.setName("远达机械制造");
        when(customerMapper.selectByIds(any())).thenReturn(List.of(c1));

        ErpSale s1 = new ErpSale();
        s1.setCustomerId(7L);
        s1.setTotalAmount(new BigDecimal("100"));
        s1.setGrossProfit(new BigDecimal("30"));
        ErpSale s2 = new ErpSale();
        s2.setCustomerId(7L);
        s2.setTotalAmount(new BigDecimal("200"));
        s2.setGrossProfit(new BigDecimal("70"));
        when(saleMapper.selectList(any())).thenReturn(List.of(s1, s2));

        DashboardPartnerRankVo vo = service.partnerRank("CUSTOMER");
        assertEquals(1, vo.getItems().size());
        assertEquals(2L, vo.getItems().get(0).getDocCount());
        assertEquals(0, new BigDecimal("300.00").compareTo(vo.getItems().get(0).getAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(vo.getItems().get(0).getGrossProfit()));
    }

    @Test
    @DisplayName("排行：供货商按本月采购额倒序")
    void partnerRankSupplier() {
        ErpSupplier s1 = new ErpSupplier();
        s1.setId(9L);
        s1.setCode("GYS01");
        s1.setName("宏发五金钢材");
        when(supplierMapper.selectByIds(any())).thenReturn(List.of(s1));

        ErpPurchase p1 = new ErpPurchase();
        p1.setSupplierId(9L);
        p1.setTotalAmount(new BigDecimal("2220"));
        when(purchaseMapper.selectList(any())).thenReturn(List.of(p1));

        FinPayment pay = new FinPayment();
        pay.setSupplierId(9L);
        pay.setAmount(new BigDecimal("300"));
        when(paymentMapper.selectList(any())).thenReturn(List.of(pay));

        DashboardPartnerRankVo vo = service.partnerRank("SUPPLIER");
        assertEquals(1, vo.getItems().size());
        DashboardPartnerRankVo.RankItem item = vo.getItems().get(0);
        assertEquals("宏发五金钢材", item.getPartyName());
        assertEquals(0, new BigDecimal("2220.00").compareTo(item.getAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(item.getPaymentAmount()));
        assertEquals(1L, item.getDocCount());
        assertNull(item.getGrossMargin());
    }

    @Test
    @DisplayName("排行：仅收款无销售的客户金额为 0，毛利率为 null")
    void partnerRankReceiptOnlyMarginNull() {
        FinReceipt receipt = new FinReceipt();
        receipt.setCustomerId(7L);
        receipt.setAmount(new BigDecimal("500"));
        when(receiptMapper.selectList(any())).thenReturn(List.of(receipt));

        DashboardPartnerRankVo vo = service.partnerRank("CUSTOMER");
        assertEquals(1, vo.getItems().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getItems().get(0).getAmount()));
        assertNull(vo.getItems().get(0).getGrossMargin());
        assertEquals(0, new BigDecimal("500.00").compareTo(vo.getItems().get(0).getReceiptAmount()));
    }

    @Test
    @DisplayName("排行：本月无往来单位时列表为空")
    void partnerRankEmpty() {
        DashboardPartnerRankVo vo = service.partnerRank("CUSTOMER");
        assertTrue(vo.getItems().isEmpty());
    }

    @Test
    @DisplayName("排行：非法单位类型抛参数异常")
    void partnerRankInvalidType() {
        assertThrows(ServiceException.class, () -> service.partnerRank("OTHER"));
    }
}
