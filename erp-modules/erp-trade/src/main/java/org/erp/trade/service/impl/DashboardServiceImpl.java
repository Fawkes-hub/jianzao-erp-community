package org.erp.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.erp.common.enums.StatusText;
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
import org.erp.stock.domain.vo.ErpStockAlertVo;
import org.erp.stock.mapper.ErpStockAlertMapper;
import org.erp.stock.mapper.ErpStockMapper;
import org.erp.trade.domain.ErpPurchase;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.domain.vo.DashboardPartnerMonthVo;
import org.erp.trade.domain.vo.DashboardPartnerRankVo;
import org.erp.trade.domain.vo.DashboardStockAlertVo;
import org.erp.trade.domain.vo.DashboardSummaryVo;
import org.erp.trade.domain.vo.DashboardTrendVo;
import org.erp.trade.mapper.ErpPurchaseMapper;
import org.erp.trade.mapper.ErpSaleMapper;
import org.erp.trade.service.IDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台看板实现（DOC/18）
 *
 * 口径：全部只统计已确认（CONFIRMED）与有效台账（非 VOID/REVERSED）；
 * 本月 = 自然月 [月初 00:00, 下月初 00:00)；成品/原料按 md_product.type 划分。
 */
@RequiredArgsConstructor
@Service
public class DashboardServiceImpl implements IDashboardService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MAX_TREND_MONTHS = 12;
    private static final int RECENT_DOC_LIMIT = 10;
    private static final String STATUS_VOID = "VOID";
    private static final String STATUS_REVERSED = "REVERSED";

    private final ErpSaleMapper saleMapper;
    private final ErpPurchaseMapper purchaseMapper;
    private final FinArApMapper arApMapper;
    private final FinReceiptMapper receiptMapper;
    private final FinPaymentMapper paymentMapper;
    private final FinCostLedgerMapper costLedgerMapper;
    private final ErpStockMapper stockMapper;
    private final ErpStockAlertMapper stockAlertMapper;
    private final ErpProductMapper productMapper;
    private final ErpCustomerMapper customerMapper;
    private final ErpSupplierMapper supplierMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryVo summary() {
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        Date monthStart = date(first);
        Date monthEnd = date(first.plusMonths(1));

        DashboardSummaryVo vo = new DashboardSummaryVo();
        vo.setMonthSales(sumSales(monthStart, monthEnd));
        vo.setMonthPurchaseAmount(sumPurchases(monthStart, monthEnd));
        vo.setMonthReceiptAmount(sumReceipts(monthStart, monthEnd));
        BigDecimal monthGrossProfit = sumSaleGrossProfit(monthStart, monthEnd);
        vo.setMonthGrossProfit(monthGrossProfit);
        // 本月销售毛利率 = Σgross_profit / Σtotal_amount × 100（与销售单毛利率同口径）
        vo.setMonthGrossMargin(vo.getMonthSales() != null
            && vo.getMonthSales().compareTo(BigDecimal.ZERO) > 0
            ? monthGrossProfit.multiply(BigDecimal.valueOf(100))
                .divide(vo.getMonthSales(), 2, RoundingMode.HALF_UP)
            : null);

        BigDecimal[] arAp = sumUnsettled();
        vo.setUnreceived(arAp[0]);
        vo.setUnpaid(arAp[1]);

        BigDecimal[] finished = sumStockByType("PRODUCT");
        vo.setFinishedStockQty(finished[0]);
        vo.setFinishedStockAmount(finished[1]);
        vo.setMaterialStockQty(sumStockByType("MATERIAL")[0]);

        vo.setDraftTodos(loadDraftTodos());
        vo.setRecentDocs(loadRecentDocs());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardTrendVo trend(Integer months) {
        int n = months == null ? 6 : months;
        if (n < 1 || n > MAX_TREND_MONTHS) {
            throw new ServiceException("趋势月数必须在 1~" + MAX_TREND_MONTHS + " 之间");
        }
        LocalDate first = LocalDate.now().withDayOfMonth(1).minusMonths(n - 1L);
        Date from = date(first);
        Date to = date(first.plusMonths(n));

        DashboardTrendVo vo = new DashboardTrendVo();
        Map<String, BigDecimal> salesByMonth = groupSalesByMonth(from, to);
        Map<String, BigDecimal> costByMonth = groupCostByMonth(from, to);
        Map<String, BigDecimal> receiptsByMonth = groupReceiptsByMonth(from, to);
        for (int i = 0; i < n; i++) {
            String m = first.plusMonths(i).format(MONTH_FMT);
            vo.getMonths().add(m);
            BigDecimal sales = salesByMonth.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal cost = costByMonth.getOrDefault(m, BigDecimal.ZERO);
            vo.getSales().add(sales);
            vo.getCost().add(cost);
            vo.getReceipts().add(receiptsByMonth.getOrDefault(m, BigDecimal.ZERO));
            vo.getProfit().add(sales.subtract(cost));
        }
        return vo;
    }

    private BigDecimal sumSales(Date from, Date to) {
        return saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
                .select(ErpSale::getTotalAmount)
                .eq(ErpSale::getStatus, "CONFIRMED")
                .ge(ErpSale::getBizDate, from)
                .lt(ErpSale::getBizDate, to))
            .stream().map(s -> nvl(s.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumSaleGrossProfit(Date from, Date to) {
        return saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
                .select(ErpSale::getGrossProfit)
                .eq(ErpSale::getStatus, "CONFIRMED")
                .ge(ErpSale::getBizDate, from)
                .lt(ErpSale::getBizDate, to))
            .stream().map(r -> r.getGrossProfit() == null ? BigDecimal.ZERO : r.getGrossProfit())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> groupSalesByMonth(Date from, Date to) {
        return groupByMonth(saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
                .select(ErpSale::getTotalAmount, ErpSale::getBizDate)
                .eq(ErpSale::getStatus, "CONFIRMED")
                .ge(ErpSale::getBizDate, from)
                .lt(ErpSale::getBizDate, to)),
            ErpSale::getBizDate, ErpSale::getTotalAmount);
    }

    private BigDecimal sumPurchases(Date from, Date to) {
        return purchaseMapper.selectList(new LambdaQueryWrapper<ErpPurchase>()
                .select(ErpPurchase::getTotalAmount)
                .eq(ErpPurchase::getStatus, "CONFIRMED")
                .ge(ErpPurchase::getBizDate, from)
                .lt(ErpPurchase::getBizDate, to))
            .stream().map(p -> nvl(p.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumReceipts(Date from, Date to) {
        return receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
                .select(FinReceipt::getAmount)
                .eq(FinReceipt::getStatus, "CONFIRMED")
                .ge(FinReceipt::getBizDate, from)
                .lt(FinReceipt::getBizDate, to))
            .stream().map(r -> nvl(r.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> groupReceiptsByMonth(Date from, Date to) {
        return groupByMonth(receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
                .select(FinReceipt::getAmount, FinReceipt::getBizDate)
                .eq(FinReceipt::getStatus, "CONFIRMED")
                .ge(FinReceipt::getBizDate, from)
                .lt(FinReceipt::getBizDate, to)),
            FinReceipt::getBizDate, FinReceipt::getAmount);
    }

    /**
     * 趋势成本与综合成本报表同口径：销售成本（biz_sale.cost_amount）+ 其他出库 + 计入成本的其他支出。
     */
    private Map<String, BigDecimal> groupCostByMonth(Date from, Date to) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (ErpSale row : saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
            .select(ErpSale::getCostAmount, ErpSale::getBizDate)
            .eq(ErpSale::getStatus, "CONFIRMED")
            .ge(ErpSale::getBizDate, from)
            .lt(ErpSale::getBizDate, to))) {
            if (row.getBizDate() == null) {
                continue;
            }
            result.merge(monthKey(row.getBizDate()), nvl(row.getCostAmount()), BigDecimal::add);
        }
        List<FinCostLedger> rows = costLedgerMapper.selectList(new LambdaQueryWrapper<FinCostLedger>()
            .select(FinCostLedger::getAmount, FinCostLedger::getBizDate)
            .eq(FinCostLedger::getStatus, "ACTIVE")
            .in(FinCostLedger::getCostType, "OTHER_OUT", "OTHER_EXPENSE")
            .ge(FinCostLedger::getBizDate, from)
            .lt(FinCostLedger::getBizDate, to));
        for (FinCostLedger row : rows) {
            if (row.getBizDate() == null) {
                continue;
            }
            String m = monthKey(row.getBizDate());
            result.merge(m, nvl(row.getAmount()), BigDecimal::add);
        }
        return result;
    }

    /**
     * @return [0]=AR 未收，[1]=AP 未付
     */
    private BigDecimal[] sumUnsettled() {
        List<FinArAp> rows = arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
            .select(FinArAp::getPartyType, FinArAp::getAmount, FinArAp::getSettledAmount)
            .ne(FinArAp::getStatus, STATUS_VOID));
        BigDecimal ar = BigDecimal.ZERO;
        BigDecimal ap = BigDecimal.ZERO;
        for (FinArAp row : rows) {
            if (STATUS_VOID.equals(row.getStatus())) {
                continue;
            }
            BigDecimal unsettled = nvl(row.getAmount()).subtract(nvl(row.getSettledAmount()));
            if ("AR".equals(row.getPartyType())) {
                ar = ar.add(unsettled);
            } else if ("AP".equals(row.getPartyType())) {
                ap = ap.add(unsettled);
            }
        }
        return new BigDecimal[]{ar.setScale(2, RoundingMode.HALF_UP), ap.setScale(2, RoundingMode.HALF_UP)};
    }

    /**
     * @return [0]=数量合计，[1]=金额合计（Σ qty × avg_cost）
     */
    private BigDecimal[] sumStockByType(String productType) {
        List<ErpStock> rows = stockMapper.selectList(new LambdaQueryWrapper<ErpStock>()
            .select(ErpStock::getProductId, ErpStock::getQty, ErpStock::getAvgCost));
        if (rows.isEmpty()) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }
        List<Long> productIds = rows.stream().map(ErpStock::getProductId).distinct().toList();
        Map<Long, String> types = new HashMap<>();
        for (ErpProduct p : productMapper.selectByIds(productIds)) {
            types.put(p.getId(), p.getType());
        }
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        for (ErpStock row : rows) {
            if (!productType.equals(types.get(row.getProductId()))) {
                continue;
            }
            BigDecimal q = nvl(row.getQty());
            qty = qty.add(q);
            amount = amount.add(q.multiply(nvl(row.getAvgCost())));
        }
        return new BigDecimal[]{qty, amount.setScale(2, RoundingMode.HALF_UP)};
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStockAlertVo stockAlert() {
        List<ErpStockAlertVo> rows = stockAlertMapper.selectAlerts(null, null);
        DashboardStockAlertVo vo = new DashboardStockAlertVo();
        List<DashboardStockAlertVo.Item> lows = new ArrayList<>();
        List<DashboardStockAlertVo.Item> highs = new ArrayList<>();
        for (ErpStockAlertVo row : rows) {
            DashboardStockAlertVo.Item item = new DashboardStockAlertVo.Item();
            item.setProductId(row.getProductId());
            item.setProductName(row.getProductName());
            item.setTotalQty(row.getTotalQty());
            item.setGap(row.getGap());
            if ("HIGH".equals(row.getAlertType())) {
                highs.add(item);
            } else {
                lows.add(item);
            }
        }
        vo.setLowCount((long) lows.size());
        vo.setHighCount((long) highs.size());
        vo.setLowTop5(lows.stream().limit(5).toList());
        vo.setHighTop5(highs.stream().limit(5).toList());
        return vo;
    }

    private DashboardSummaryVo.DraftTodos loadDraftTodos() {
        DashboardSummaryVo.DraftTodos todos = new DashboardSummaryVo.DraftTodos();
        todos.setSaleDrafts(saleMapper.selectCount(new LambdaQueryWrapper<ErpSale>()
            .eq(ErpSale::getStatus, "DRAFT")));
        todos.setPurchaseDrafts(purchaseMapper.selectCount(new LambdaQueryWrapper<ErpPurchase>()
            .eq(ErpPurchase::getStatus, "DRAFT")));
        todos.setReceiptDrafts(receiptMapper.selectCount(new LambdaQueryWrapper<FinReceipt>()
            .eq(FinReceipt::getStatus, "DRAFT")));
        todos.setPaymentDrafts(paymentMapper.selectCount(new LambdaQueryWrapper<FinPayment>()
            .eq(FinPayment::getStatus, "DRAFT")));
        return todos;
    }

    private List<DashboardSummaryVo.RecentDoc> loadRecentDocs() {
        List<DashboardSummaryVo.RecentDoc> docs = new ArrayList<>();
        for (ErpSale s : saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
                .select(ErpSale::getNo, ErpSale::getBizDate, ErpSale::getTotalAmount, ErpSale::getStatus, ErpSale::getId)
                .orderByDesc(ErpSale::getId)
                .last("limit " + RECENT_DOC_LIMIT))) {
            docs.add(recentDoc("SALE", s.getNo(), s.getBizDate(), s.getTotalAmount(), s.getStatus()));
        }
        for (ErpPurchase p : purchaseMapper.selectList(new LambdaQueryWrapper<ErpPurchase>()
                .select(ErpPurchase::getNo, ErpPurchase::getBizDate, ErpPurchase::getTotalAmount, ErpPurchase::getStatus, ErpPurchase::getId)
                .orderByDesc(ErpPurchase::getId)
                .last("limit " + RECENT_DOC_LIMIT))) {
            docs.add(recentDoc("PURCHASE", p.getNo(), p.getBizDate(), p.getTotalAmount(), p.getStatus()));
        }
        for (FinReceipt r : receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
                .select(FinReceipt::getNo, FinReceipt::getBizDate, FinReceipt::getAmount, FinReceipt::getStatus, FinReceipt::getId)
                .orderByDesc(FinReceipt::getId)
                .last("limit " + RECENT_DOC_LIMIT))) {
            docs.add(recentDoc("RECEIPT", r.getNo(), r.getBizDate(), r.getAmount(), r.getStatus()));
        }
        for (FinPayment pay : paymentMapper.selectList(new LambdaQueryWrapper<FinPayment>()
                .select(FinPayment::getNo, FinPayment::getBizDate, FinPayment::getAmount, FinPayment::getStatus, FinPayment::getId)
                .orderByDesc(FinPayment::getId)
                .last("limit " + RECENT_DOC_LIMIT))) {
            docs.add(recentDoc("PAYMENT", pay.getNo(), pay.getBizDate(), pay.getAmount(), pay.getStatus()));
        }
        docs.sort(Comparator.comparing(DashboardSummaryVo.RecentDoc::getBizDate,
            Comparator.nullsLast(Comparator.reverseOrder())));
        return docs.size() > RECENT_DOC_LIMIT ? docs.subList(0, RECENT_DOC_LIMIT) : docs;
    }

    private DashboardSummaryVo.RecentDoc recentDoc(String type, String no, Date bizDate, BigDecimal amount, String status) {
        DashboardSummaryVo.RecentDoc doc = new DashboardSummaryVo.RecentDoc();
        doc.setType(type);
        doc.setNo(no);
        doc.setBizDate(bizDate);
        doc.setAmount(amount);
        doc.setStatus(status);
        doc.setStatusText(StatusText.bill(status));
        return doc;
    }

    private <T> Map<String, BigDecimal> groupByMonth(List<T> rows,
                                                     java.util.function.Function<T, Date> dateFn,
                                                     java.util.function.Function<T, BigDecimal> amountFn) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (T row : rows) {
            Date d = dateFn.apply(row);
            if (d == null) {
                continue;
            }
            result.merge(monthKey(d), nvl(amountFn.apply(row)), BigDecimal::add);
        }
        return result;
    }

    private String monthKey(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(MONTH_FMT);
    }

    private Date date(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 往来单位当月明细：客户看本月销售与收款，供货商看本月采购与付款。
     *
     * 口径与看板 summary 一致——只统计 CONFIRMED 单据，bizDate 落在自然月窗口内。
     *
     * @param partyType CUSTOMER=客户，SUPPLIER=供货商
     * @param partyId   往来单位 ID
     * @return 当月汇总数字与单据明细，单位不存在时名称为空、数字为 0
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardPartnerMonthVo partnerMonth(String partyType, Long partyId) {
        if (partyId == null) {
            throw new ServiceException("请选择往来单位");
        }
        boolean customer = "CUSTOMER".equals(partyType);
        if (!customer && !"SUPPLIER".equals(partyType)) {
            throw new ServiceException("往来单位类型只能是 CUSTOMER 或 SUPPLIER");
        }
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        Date from = date(first);
        Date to = date(first.plusMonths(1));

        DashboardPartnerMonthVo vo = new DashboardPartnerMonthVo();
        vo.setPartyType(partyType);
        vo.setPartyId(partyId);
        vo.setMonth(first.format(MONTH_FMT));
        vo.setSalesAmount(BigDecimal.ZERO);
        vo.setPurchaseAmount(BigDecimal.ZERO);
        vo.setReceiptAmount(BigDecimal.ZERO);
        vo.setPaymentAmount(BigDecimal.ZERO);
        vo.setGrossProfit(BigDecimal.ZERO);

        if (customer) {
            fillCustomerMonth(vo, partyId, from, to);
        } else {
            fillSupplierMonth(vo, partyId, from, to);
        }

        vo.getDocs().sort(Comparator.comparing(DashboardPartnerMonthVo.Doc::getBizDate,
            Comparator.nullsLast(Comparator.reverseOrder())));
        return vo;
    }

    /**
     * 往来单位本月排行：按本月发生额倒序列出各客户/供货商。
     *
     * 口径与 partnerMonth 一致——只统计 CONFIRMED 单据，bizDate 落在自然月窗口内。
     *
     * @param partyType CUSTOMER=客户，SUPPLIER=供货商
     * @return 本月排行条目，按金额倒序；本月无往来单位时列表为空
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardPartnerRankVo partnerRank(String partyType) {
        boolean customer = "CUSTOMER".equals(partyType);
        if (!customer && !"SUPPLIER".equals(partyType)) {
            throw new ServiceException("往来单位类型只能是 CUSTOMER 或 SUPPLIER");
        }
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        Date from = date(first);
        Date to = date(first.plusMonths(1));

        DashboardPartnerRankVo vo = new DashboardPartnerRankVo();
        vo.setPartyType(partyType);
        vo.setMonth(first.format(MONTH_FMT));

        Map<Long, DashboardPartnerRankVo.RankItem> items = new HashMap<>();
        if (customer) {
            for (ErpSale sale : saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
                .select(ErpSale::getCustomerId, ErpSale::getTotalAmount, ErpSale::getGrossProfit)
                .eq(ErpSale::getStatus, "CONFIRMED")
                .ge(ErpSale::getBizDate, from)
                .lt(ErpSale::getBizDate, to))) {
                if (sale.getCustomerId() == null) {
                    continue;
                }
                DashboardPartnerRankVo.RankItem item = items.computeIfAbsent(sale.getCustomerId(),
                    id -> newRankItem(id));
                item.setAmount(item.getAmount().add(nvl(sale.getTotalAmount())));
                item.setGrossProfit(item.getGrossProfit().add(nvl(sale.getGrossProfit())));
                item.setDocCount(item.getDocCount() + 1);
            }
            for (FinReceipt receipt : receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
                .select(FinReceipt::getCustomerId, FinReceipt::getAmount)
                .eq(FinReceipt::getStatus, "CONFIRMED")
                .ge(FinReceipt::getBizDate, from)
                .lt(FinReceipt::getBizDate, to))) {
                if (receipt.getCustomerId() == null) {
                    continue;
                }
                DashboardPartnerRankVo.RankItem item = items.computeIfAbsent(receipt.getCustomerId(),
                    id -> newRankItem(id));
                item.setReceiptAmount(item.getReceiptAmount().add(nvl(receipt.getAmount())));
            }
            fillCustomerNames(items);
        } else {
            for (ErpPurchase purchase : purchaseMapper.selectList(new LambdaQueryWrapper<ErpPurchase>()
                .select(ErpPurchase::getSupplierId, ErpPurchase::getTotalAmount)
                .eq(ErpPurchase::getStatus, "CONFIRMED")
                .ge(ErpPurchase::getBizDate, from)
                .lt(ErpPurchase::getBizDate, to))) {
                if (purchase.getSupplierId() == null) {
                    continue;
                }
                DashboardPartnerRankVo.RankItem item = items.computeIfAbsent(purchase.getSupplierId(),
                    id -> newRankItem(id));
                item.setAmount(item.getAmount().add(nvl(purchase.getTotalAmount())));
                item.setDocCount(item.getDocCount() + 1);
            }
            for (FinPayment payment : paymentMapper.selectList(new LambdaQueryWrapper<FinPayment>()
                .select(FinPayment::getSupplierId, FinPayment::getAmount)
                .eq(FinPayment::getStatus, "CONFIRMED")
                .ge(FinPayment::getBizDate, from)
                .lt(FinPayment::getBizDate, to))) {
                if (payment.getSupplierId() == null) {
                    continue;
                }
                DashboardPartnerRankVo.RankItem item = items.computeIfAbsent(payment.getSupplierId(),
                    id -> newRankItem(id));
                item.setPaymentAmount(item.getPaymentAmount().add(nvl(payment.getAmount())));
            }
            fillSupplierNames(items);
        }

        List<DashboardPartnerRankVo.RankItem> list = new ArrayList<>(items.values());
        list.sort(Comparator.comparing(DashboardPartnerRankVo.RankItem::getAmount).reversed()
            .thenComparing(DashboardPartnerRankVo.RankItem::getPartyId,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (DashboardPartnerRankVo.RankItem item : list) {
            item.setAmount(item.getAmount().setScale(2, RoundingMode.HALF_UP));
            item.setGrossProfit(item.getGrossProfit().setScale(2, RoundingMode.HALF_UP));
            item.setReceiptAmount(item.getReceiptAmount().setScale(2, RoundingMode.HALF_UP));
            item.setPaymentAmount(item.getPaymentAmount().setScale(2, RoundingMode.HALF_UP));
            // 毛利率只对客户有意义：供货商无成本快照口径，保持 null
            if (customer) {
                item.setGrossMargin(item.getAmount().compareTo(BigDecimal.ZERO) > 0
                    ? item.getGrossProfit().multiply(BigDecimal.valueOf(100))
                        .divide(item.getAmount(), 2, RoundingMode.HALF_UP)
                    : null);
            }
        }
        vo.getItems().addAll(list);
        return vo;
    }

    /**
     * 新建排行条目，金额字段初始化为 0，避免累加时 NPE。
     *
     * @param partyId 往来单位 ID
     * @return 初始化的排行条目
     */
    private DashboardPartnerRankVo.RankItem newRankItem(Long partyId) {
        DashboardPartnerRankVo.RankItem item = new DashboardPartnerRankVo.RankItem();
        item.setPartyId(partyId);
        item.setAmount(BigDecimal.ZERO);
        item.setGrossProfit(BigDecimal.ZERO);
        item.setReceiptAmount(BigDecimal.ZERO);
        item.setPaymentAmount(BigDecimal.ZERO);
        item.setDocCount(0L);
        return item;
    }

    /**
     * 回填客户名称与编码，主数据中已删除的往来单位保留空名。
     *
     * @param items 排行条目映射
     */
    private void fillCustomerNames(Map<Long, DashboardPartnerRankVo.RankItem> items) {
        if (items.isEmpty()) {
            return;
        }
        List<ErpCustomer> customers = customerMapper.selectByIds(items.keySet());
        for (ErpCustomer customer : customers) {
            DashboardPartnerRankVo.RankItem item = items.get(customer.getId());
            if (item == null) {
                continue;
            }
            item.setPartyName(customer.getName());
            item.setPartyCode(customer.getCode());
        }
    }

    /**
     * 回填供货商名称与编码，主数据中已删除的往来单位保留空名。
     *
     * @param items 排行条目映射
     */
    private void fillSupplierNames(Map<Long, DashboardPartnerRankVo.RankItem> items) {
        if (items.isEmpty()) {
            return;
        }
        List<ErpSupplier> suppliers = supplierMapper.selectByIds(items.keySet());
        for (ErpSupplier supplier : suppliers) {
            DashboardPartnerRankVo.RankItem item = items.get(supplier.getId());
            if (item == null) {
                continue;
            }
            item.setPartyName(supplier.getName());
            item.setPartyCode(supplier.getCode());
        }
    }

    /**
     * 客户口径：本月销售单数、销售额、毛利、毛利率与收款，并列出销售单与收款单。
     *
     * @param vo    汇总结果容器
     * @param partyId 客户 ID
     * @param from  月初 00:00（含）
     * @param to    下月初 00:00（不含）
     */
    private void fillCustomerMonth(DashboardPartnerMonthVo vo, Long partyId, Date from, Date to) {
        ErpCustomer customer = customerMapper.selectById(partyId);
        vo.setPartyName(customer == null ? null : customer.getName());

        List<ErpSale> sales = saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
            .eq(ErpSale::getCustomerId, partyId)
            .eq(ErpSale::getStatus, "CONFIRMED")
            .ge(ErpSale::getBizDate, from)
            .lt(ErpSale::getBizDate, to)
            .orderByDesc(ErpSale::getBizDate));
        BigDecimal salesAmount = BigDecimal.ZERO;
        BigDecimal grossProfit = BigDecimal.ZERO;
        for (ErpSale sale : sales) {
            salesAmount = salesAmount.add(nvl(sale.getTotalAmount()));
            grossProfit = grossProfit.add(nvl(sale.getGrossProfit()));
            DashboardPartnerMonthVo.Doc doc = new DashboardPartnerMonthVo.Doc();
            doc.setType("SALE");
            doc.setNo(sale.getNo());
            doc.setBizDate(sale.getBizDate());
            doc.setAmount(nvl(sale.getTotalAmount()));
            doc.setGrossProfit(nvl(sale.getGrossProfit()));
            doc.setRemark(sale.getRemark());
            vo.getDocs().add(doc);
        }
        vo.setDocCount((long) sales.size());
        vo.setSalesAmount(salesAmount.setScale(2, RoundingMode.HALF_UP));
        vo.setGrossProfit(grossProfit.setScale(2, RoundingMode.HALF_UP));
        vo.setGrossMargin(salesAmount.compareTo(BigDecimal.ZERO) > 0
            ? grossProfit.multiply(BigDecimal.valueOf(100)).divide(salesAmount, 2, RoundingMode.HALF_UP)
            : null);

        List<FinReceipt> receipts = receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
            .eq(FinReceipt::getCustomerId, partyId)
            .eq(FinReceipt::getStatus, "CONFIRMED")
            .ge(FinReceipt::getBizDate, from)
            .lt(FinReceipt::getBizDate, to)
            .orderByDesc(FinReceipt::getBizDate));
        BigDecimal receiptAmount = BigDecimal.ZERO;
        for (FinReceipt receipt : receipts) {
            receiptAmount = receiptAmount.add(nvl(receipt.getAmount()));
            DashboardPartnerMonthVo.Doc doc = new DashboardPartnerMonthVo.Doc();
            doc.setType("RECEIPT");
            doc.setNo(receipt.getNo());
            doc.setBizDate(receipt.getBizDate());
            doc.setAmount(nvl(receipt.getAmount()));
            doc.setRemark(receipt.getRemark());
            vo.getDocs().add(doc);
        }
        vo.setReceiptAmount(receiptAmount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 供货商口径：本月采购单数、采购额（含运杂费）与付款，并列出采购单与付款单。
     *
     * @param vo    汇总结果容器
     * @param partyId 供货商 ID
     * @param from  月初 00:00（含）
     * @param to    下月初 00:00（不含）
     */
    private void fillSupplierMonth(DashboardPartnerMonthVo vo, Long partyId, Date from, Date to) {
        ErpSupplier supplier = supplierMapper.selectById(partyId);
        vo.setPartyName(supplier == null ? null : supplier.getName());

        List<ErpPurchase> purchases = purchaseMapper.selectList(new LambdaQueryWrapper<ErpPurchase>()
            .eq(ErpPurchase::getSupplierId, partyId)
            .eq(ErpPurchase::getStatus, "CONFIRMED")
            .ge(ErpPurchase::getBizDate, from)
            .lt(ErpPurchase::getBizDate, to)
            .orderByDesc(ErpPurchase::getBizDate));
        BigDecimal purchaseAmount = BigDecimal.ZERO;
        for (ErpPurchase purchase : purchases) {
            purchaseAmount = purchaseAmount.add(nvl(purchase.getTotalAmount()));
            DashboardPartnerMonthVo.Doc doc = new DashboardPartnerMonthVo.Doc();
            doc.setType("PURCHASE");
            doc.setNo(purchase.getNo());
            doc.setBizDate(purchase.getBizDate());
            doc.setAmount(nvl(purchase.getTotalAmount()));
            doc.setRemark(purchase.getRemark());
            vo.getDocs().add(doc);
        }
        vo.setDocCount((long) purchases.size());
        vo.setPurchaseAmount(purchaseAmount.setScale(2, RoundingMode.HALF_UP));

        List<FinPayment> payments = paymentMapper.selectList(new LambdaQueryWrapper<FinPayment>()
            .eq(FinPayment::getSupplierId, partyId)
            .eq(FinPayment::getStatus, "CONFIRMED")
            .ge(FinPayment::getBizDate, from)
            .lt(FinPayment::getBizDate, to)
            .orderByDesc(FinPayment::getBizDate));
        BigDecimal paymentAmount = BigDecimal.ZERO;
        for (FinPayment payment : payments) {
            paymentAmount = paymentAmount.add(nvl(payment.getAmount()));
            DashboardPartnerMonthVo.Doc doc = new DashboardPartnerMonthVo.Doc();
            doc.setType("PAYMENT");
            doc.setNo(payment.getNo());
            doc.setBizDate(payment.getBizDate());
            doc.setAmount(nvl(payment.getAmount()));
            doc.setRemark(payment.getRemark());
            vo.getDocs().add(doc);
        }
        vo.setPaymentAmount(paymentAmount.setScale(2, RoundingMode.HALF_UP));
    }
}
