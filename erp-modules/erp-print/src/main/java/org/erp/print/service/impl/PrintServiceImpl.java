package org.erp.print.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.erp.fin.domain.FinArAp;
import org.erp.fin.domain.FinOpening;
import org.erp.fin.domain.FinPayment;
import org.erp.fin.domain.FinPaymentAlloc;
import org.erp.fin.domain.FinReceipt;
import org.erp.fin.domain.FinReceiptAlloc;
import org.erp.fin.mapper.FinArApMapper;
import org.erp.fin.mapper.FinOpeningMapper;
import org.erp.fin.mapper.FinPaymentAllocMapper;
import org.erp.fin.mapper.FinPaymentMapper;
import org.erp.fin.mapper.FinReceiptAllocMapper;
import org.erp.fin.mapper.FinReceiptMapper;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpProduct;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpProductMapper;
import org.erp.master.mapper.ErpSupplierMapper;
import org.erp.print.domain.PrintTemplate;
import org.erp.print.service.IPrintService;
import org.erp.print.service.IPrintTemplateService;
import org.erp.trade.domain.ErpPurchase;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.domain.ErpSaleItem;
import org.erp.trade.mapper.ErpPurchaseMapper;
import org.erp.trade.mapper.ErpSaleItemMapper;
import org.erp.trade.mapper.ErpSaleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打印渲染服务实现
 *
 * 场景（DOC/12 §9、DOC/14）：送货单 / 对账单 / 收款单 / 付款单。
 * 模板解析：指定 templateId → 客户专属 → 租户默认 → 内置兜底。
 * 口径：往月累计应收 + 本期销售净额 − 本期收款 = 期末应收。
 */
@RequiredArgsConstructor
@Service
public class PrintServiceImpl implements IPrintService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STATUS_VOID = "VOID";
    private static final int MAX_COPIES = 10;

    private final IPrintTemplateService templateService;
    private final ErpSaleMapper saleMapper;
    private final FinReceiptMapper receiptMapper;
    private final FinReceiptAllocMapper receiptAllocMapper;
    private final FinPaymentMapper paymentMapper;
    private final FinPaymentAllocMapper paymentAllocMapper;
    private final FinArApMapper arApMapper;
    private final FinOpeningMapper openingMapper;
    private final ErpCustomerMapper customerMapper;
    private final ErpSupplierMapper supplierMapper;
    private final ErpProductMapper productMapper;
    private final ErpSaleItemMapper saleItemMapper;
    private final ErpPurchaseMapper purchaseMapper;

    @Override
    @Transactional(readOnly = true)
    public String renderSaleDelivery(Long saleId, Long templateId, boolean showProfit, int copies) {
        ErpSale sale = saleMapper.selectById(saleId);
        if (sale == null) {
            throw new ServiceException("销售单不存在：" + saleId);
        }
        PrintContext ctx = new PrintContext();
        ctx.setCopies(normalizeCopies(copies));
        fillPartner(ctx, "AR", sale.getCustomerId());
        ctx.getDoc().setNo(sale.getNo());
        ctx.getDoc().setDate(sale.getBizDate());
        ctx.getDoc().setType("SALE_DELIVERY");
        ctx.getDoc().setRemark(sale.getRemark());
        ctx.getDoc().setStatus(sale.getStatus());

        BigDecimal qtyTotal = BigDecimal.ZERO;
        List<PrintContext.Item> items = new ArrayList<>();
        for (ErpSaleVoRow row : loadSaleItems(saleId)) {
            PrintContext.Item item = new PrintContext.Item();
            item.setIndex(items.size() + 1);
            item.setName(row.name);
            item.setSpec(row.spec);
            item.setUnit(row.unit);
            item.setQty(row.qty);
            item.setPrice(row.price);
            item.setAmount(row.amount);
            item.setTaxType(row.taxType);
            item.setRemark(row.remark);
            items.add(item);
            qtyTotal = qtyTotal.add(row.qty == null ? BigDecimal.ZERO : row.qty);
        }
        ctx.setItems(items);
        ctx.getTotals().setQtyTotal(qtyTotal);
        ctx.getTotals().setAmountTotal(sale.getTotalAmount());
        ctx.getTotals().setAmountUpper(PrintFormatUtils.upper(sale.getTotalAmount()));
        if (showProfit) {
            PrintContext.Profit profit = new PrintContext.Profit();
            profit.setCostAmount(sale.getCostAmount());
            profit.setGrossProfit(sale.getGrossProfit());
            profit.setGrossMargin(sale.getGrossMargin());
            ctx.setProfit(profit);
        }
        ctx.setShowProfit(showProfit);
        return render(ctx, templateId, "SALE_DELIVERY", sale.getCustomerId());
    }

    @Override
    @Transactional(readOnly = true)
    public String renderStatement(Long customerId, String month, Long templateId) {
        if (month == null || month.isBlank()) {
            month = LocalDate.now().format(MONTH_FMT);
        }
        LocalDate first;
        try {
            first = LocalDate.parse(month + "-01");
        } catch (Exception e) {
            throw new ServiceException("账期格式必须是 yyyy-MM");
        }
        LocalDate next = first.plusMonths(1);
        Date from = Date.from(first.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date to = Date.from(next.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 历史台账含期初、销售及退货；收款按收款日期扣除，避免按当前核销状态回溯。
        BigDecimal previousReceivable = BigDecimal.ZERO;
        BigDecimal historicalSales = BigDecimal.ZERO;
        BigDecimal historicalReceipts = BigDecimal.ZERO;
        for (FinArAp row : arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
                .eq(FinArAp::getPartyType, "AR")
                .eq(FinArAp::getPartyId, customerId)
                .ne(FinArAp::getStatus, STATUS_VOID)
                .lt(FinArAp::getBizDate, from))) {
            previousReceivable = previousReceivable.add(nvl(row.getAmount()));
            if ("SALE".equals(row.getSourceType()) || "SALE_RETURN".equals(row.getSourceType())) {
                historicalSales = historicalSales.add(nvl(row.getAmount()));
            }
        }
        for (FinReceipt receipt : receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
                .eq(FinReceipt::getCustomerId, customerId)
                .eq(FinReceipt::getStatus, "CONFIRMED")
                .lt(FinReceipt::getBizDate, from))) {
            previousReceivable = previousReceivable.subtract(nvl(receipt.getAmount()));
            historicalReceipts = historicalReceipts.add(nvl(receipt.getAmount()));
        }

        // 本期销售（单据日期），已确认；按销售明细展开商品行
        BigDecimal salesAmount = BigDecimal.ZERO;
        List<PrintContext.StatementItem> statementItems = new ArrayList<>();
        for (ErpSale sale : saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
                .eq(ErpSale::getCustomerId, customerId)
                .eq(ErpSale::getStatus, "CONFIRMED")
                .ge(ErpSale::getBizDate, from)
                .lt(ErpSale::getBizDate, to)
                .orderByAsc(ErpSale::getBizDate)
                .orderByAsc(ErpSale::getId))) {
            salesAmount = salesAmount.add(nvl(sale.getTotalAmount()));
            List<ErpSaleItem> saleItems = saleItemMapper.selectList(new LambdaQueryWrapper<ErpSaleItem>()
                .eq(ErpSaleItem::getOrderId, sale.getId())
                .orderByAsc(ErpSaleItem::getSeq));
            if (saleItems.isEmpty()) {
                statementItems.add(statementItem(sale.getBizDate(), sale.getNo(),
                    "销售", null, nvl(sale.getTotalAmount()), null));
                continue;
            }
            for (ErpSaleItem saleItem : saleItems) {
                ErpProduct product = saleItem.getProductId() == null ? null : productMapper.selectById(saleItem.getProductId());
                PrintContext.StatementItem si = statementItem(sale.getBizDate(), sale.getNo(),
                    "销售", product == null ? null : product.getName(), nvl(saleItem.getAmount()), saleItem.getQty());
                si.setPrice(saleItem.getPrice());
                // 数量按商品精度展示：整数商品输出 1 而不是 1.000；商品已删除或精度缺失回落 3 位保真
                if (saleItem.getQty() != null) {
                    int qtyScale = product == null || product.getQtyScale() == null ? 3 : Math.min(3, Math.max(0, product.getQtyScale()));
                    si.setQtyText(saleItem.getQty().setScale(qtyScale, RoundingMode.HALF_UP).toPlainString());
                }
                statementItems.add(si);
            }
        }

        // 销售退货台账为负数，计入本期销售净额并在明细中展示。
        for (FinArAp row : arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
                .eq(FinArAp::getPartyType, "AR")
                .eq(FinArAp::getPartyId, customerId)
                .eq(FinArAp::getSourceType, "SALE_RETURN")
                .ne(FinArAp::getStatus, STATUS_VOID)
                .ge(FinArAp::getBizDate, from)
                .lt(FinArAp::getBizDate, to))) {
            salesAmount = salesAmount.add(nvl(row.getAmount()));
            statementItems.add(statementItem(row.getBizDate(), "销售退货", "销售退货", null, nvl(row.getAmount()), null));
        }

        // 本期收款（收款日期，已确认）
        BigDecimal receiptAmount = BigDecimal.ZERO;
        for (FinReceipt receipt : receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
                .eq(FinReceipt::getCustomerId, customerId)
                .eq(FinReceipt::getStatus, "CONFIRMED")
                .ge(FinReceipt::getBizDate, from)
                .lt(FinReceipt::getBizDate, to))) {
            receiptAmount = receiptAmount.add(nvl(receipt.getAmount()));
            statementItems.add(statementItem(receipt.getBizDate(), receipt.getNo(),
                "收款", null, nvl(receipt.getAmount()).negate(), null));
        }
        statementItems.sort((a, b) -> {
            int c = a.getDate().compareTo(b.getDate());
            return c != 0 ? c : a.getNo().compareTo(b.getNo());
        });

        // 累计未付按所选月末截止；旧字段累计应收仍按当天截止。
        Date tomorrow = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        BigDecimal totalReceivable = BigDecimal.ZERO;
        BigDecimal cumulativeUnpaid = BigDecimal.ZERO;
        for (FinArAp row : arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
                .eq(FinArAp::getPartyType, "AR")
                .eq(FinArAp::getPartyId, customerId)
                .ne(FinArAp::getStatus, STATUS_VOID)
                .lt(FinArAp::getBizDate, to.after(tomorrow) ? to : tomorrow))) {
            if (row.getBizDate().before(to)) cumulativeUnpaid = cumulativeUnpaid.add(nvl(row.getAmount()));
            if (row.getBizDate().before(tomorrow)) totalReceivable = totalReceivable.add(nvl(row.getAmount()));
        }
        for (FinReceipt receipt : receiptMapper.selectList(new LambdaQueryWrapper<FinReceipt>()
                .eq(FinReceipt::getCustomerId, customerId)
                .eq(FinReceipt::getStatus, "CONFIRMED")
                .lt(FinReceipt::getBizDate, to.after(tomorrow) ? to : tomorrow))) {
            if (receipt.getBizDate().before(to)) cumulativeUnpaid = cumulativeUnpaid.subtract(nvl(receipt.getAmount()));
            if (receipt.getBizDate().before(tomorrow)) totalReceivable = totalReceivable.subtract(nvl(receipt.getAmount()));
        }

        BigDecimal closing = previousReceivable.add(salesAmount).subtract(receiptAmount);

        PrintContext ctx = new PrintContext();
        fillPartner(ctx, "AR", customerId);
        ctx.getDoc().setType("STATEMENT");
        PrintContext.Statement statement = new PrintContext.Statement();
        statement.setMonth(month);
        statement.setPreviousReceivable(previousReceivable.setScale(2, RoundingMode.HALF_UP));
        statement.setOpening(statement.getPreviousReceivable());
        statement.setSalesAmount(salesAmount.setScale(2, RoundingMode.HALF_UP));
        statement.setReceiptAmount(receiptAmount.setScale(2, RoundingMode.HALF_UP));
        statement.setMonthlyUnpaid(salesAmount.subtract(receiptAmount).setScale(2, RoundingMode.HALF_UP));
        statement.setCumulativeSales(historicalSales.add(salesAmount).setScale(2, RoundingMode.HALF_UP));
        statement.setCumulativeReceipts(historicalReceipts.add(receiptAmount).setScale(2, RoundingMode.HALF_UP));
        statement.setCumulativeUnpaid(cumulativeUnpaid.setScale(2, RoundingMode.HALF_UP));
        statement.setClosing(closing.setScale(2, RoundingMode.HALF_UP));
        statement.setTotalReceivable(totalReceivable.setScale(2, RoundingMode.HALF_UP));
        statement.setItems(statementItems);
        ctx.setStatement(statement);
        return render(ctx, templateId, "STATEMENT", customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public String renderReceipt(Long receiptId, Long templateId) {
        FinReceipt receipt = receiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new ServiceException("收款单不存在：" + receiptId);
        }
        return renderReceiptPayment(receipt.getNo(), "RECEIPT", receipt.getCustomerId(),
            receipt.getAmount(), receipt.getBizDate(), receipt.getAccount(), receipt.getRemark(),
            loadAllocSourceNos(receiptAllocMapper.selectList(new LambdaQueryWrapper<FinReceiptAlloc>()
                .eq(FinReceiptAlloc::getReceiptId, receiptId))),
            templateId, receipt.getCustomerId());
    }

    @Override
    @Transactional(readOnly = true)
    public String renderPayment(Long paymentId, Long templateId) {
        FinPayment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new ServiceException("付款单不存在：" + paymentId);
        }
        return renderReceiptPayment(payment.getNo(), "PAYMENT", payment.getSupplierId(),
            payment.getAmount(), payment.getBizDate(), payment.getAccount(), payment.getRemark(),
            loadAllocSourceNos(paymentAllocMapper.selectList(new LambdaQueryWrapper<FinPaymentAlloc>()
                .eq(FinPaymentAlloc::getPaymentId, paymentId))),
            templateId, null);
    }

    @Override
    public String preview(String scene, String content, Long customerId, boolean showProfit, String paper) {
        PrintContext ctx = sampleContext(scene, showProfit);
        String rendered = ThymeleafRenderer.render(content, toModel(ctx));
        // 预览与正式渲染同一纸张规则：模板自带 @page 时尊重原设定，否则按 paper 注入
        return PageSetup.injectIntoHead(rendered, PageSetup.pageStyle(paper, rendered));
    }

    private String renderReceiptPayment(String no, String type, Long partyId, BigDecimal amount,
                                        Date bizDate, String account, String remark,
                                        List<PrintContext.Item> items, Long templateId, Long customerId) {
        PrintContext ctx = new PrintContext();
        fillPartner(ctx, "PAYMENT".equals(type) ? "AP" : "AR", partyId);
        ctx.getDoc().setNo(no);
        ctx.getDoc().setType(type);
        ctx.getDoc().setDate(bizDate);
        ctx.getDoc().setAccount(account);
        ctx.getDoc().setRemark(remark);
        ctx.setItems(items);
        ctx.getTotals().setAmountTotal(amount);
        ctx.getTotals().setAmountUpper(PrintFormatUtils.upper(amount));
        return render(ctx, templateId, type, customerId);
    }

    private String render(PrintContext ctx, Long templateId, String scene, Long customerId) {
        PrintTemplate template = templateService.resolve(templateId, scene, customerId);
        if (template == null) {
            template = builtin(scene);
        }
        ctx.setPrintDate(LocalDate.now().format(DATE_FMT));
        String rendered = ThymeleafRenderer.render(template.getContent(), toModel(ctx));
        // paper 落地：模板未自带 @page 时按模板纸张生成并注入 head，避免针打实际装纸
        // （如 241×140）与页面声明 A4 不一致时浏览器等比缩小版面；自带 @page 的模板尊重原设定
        String withPage = PageSetup.injectIntoHead(rendered, PageSetup.pageStyle(template.getPaper(), rendered));
        return ThymeleafRenderer.renderWithCopies(withPage, ctx.getCopies() == null ? 1 : ctx.getCopies());
    }

    /**
     * 份数钳制在 1..MAX_COPIES，防止放大渲染资源
     */
    private int normalizeCopies(Integer copies) {
        return copies == null ? 1 : Math.max(1, Math.min(copies, MAX_COPIES));
    }

    private PrintTemplate builtin(String scene) {
        return switch (scene) {
            case "SALE_DELIVERY" -> BuiltinTemplates.saleDelivery();
            case "STATEMENT" -> BuiltinTemplates.statement();
            case "RECEIPT", "PAYMENT" -> BuiltinTemplates.receiptPayment();
            default -> throw new ServiceException("该场景暂不支持打印：" + scene);
        };
    }

    private Map<String, Object> toModel(PrintContext ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("tenant", ctx.getTenant());
        model.put("doc", ctx.getDoc());
        model.put("partner", ctx.getPartner());
        model.put("items", ctx.getItems());
        model.put("totals", ctx.getTotals());
        model.put("statement", ctx.getStatement());
        model.put("profit", ctx.getProfit());
        model.put("signatures", ctx.getSignatures());
        model.put("printDate", ctx.getPrintDate());
        model.put("copies", ctx.getCopies());
        model.put("showProfit", ctx.getShowProfit());
        return model;
    }

    private PrintContext sampleContext(String scene, boolean showProfit) {
        PrintContext ctx = new PrintContext();
        ctx.getTenant().setName("示例加工厂");
        ctx.getPartner().setName("示例机械有限公司");
        ctx.getPartner().setContact("张三");
        ctx.getPartner().setPhone("13800000000");
        ctx.getPartner().setAddress("示例市示例区示例路 1 号");
        ctx.getDoc().setNo("XS202610310001");
        ctx.getDoc().setDate(new Date());
        ctx.getDoc().setAccount("微信");
        ctx.getDoc().setRemark("示例备注");
        ctx.getDoc().setType("SALE_DELIVERY".equals(scene) ? "SALE_DELIVERY"
            : "RECEIPT".equals(scene) ? "RECEIPT" : "PAYMENT".equals(scene) ? "PAYMENT" : scene);
        PrintContext.Item item = new PrintContext.Item();
        item.setIndex(1);
        item.setName("料管");
        item.setSpec("Φ25×2m");
        item.setUnit("支");
        item.setQty(BigDecimal.TEN);
        item.setPrice(new BigDecimal("50.00"));
        item.setAmount(new BigDecimal("500.00"));
        item.setNo("XS202610050001");
        item.setDate(new Date());
        ctx.setItems(List.of(item));
        ctx.getTotals().setAmountTotal(new BigDecimal("500.00"));
        ctx.getTotals().setAmountUpper(PrintFormatUtils.upper(new BigDecimal("500.00")));
        // 预览示例数据无条件填充全部变量：模板场景与内容可能不一致（如送货单场景套用对账单模板），
        // 若按场景只给对应变量，模板引用未填充对象会抛 OGNL null 异常
        PrintContext.Statement statement = new PrintContext.Statement();
        statement.setMonth(LocalDate.now().format(MONTH_FMT));
        statement.setOpening(new BigDecimal("2000.00"));
        statement.setPreviousReceivable(new BigDecimal("2000.00"));
        statement.setSalesAmount(new BigDecimal("500.00"));
        statement.setReceiptAmount(new BigDecimal("300.00"));
        statement.setMonthlyUnpaid(new BigDecimal("200.00"));
        statement.setCumulativeSales(new BigDecimal("2500.00"));
        statement.setCumulativeReceipts(new BigDecimal("300.00"));
        statement.setCumulativeUnpaid(new BigDecimal("2200.00"));
        statement.setClosing(new BigDecimal("2200.00"));
        statement.setTotalReceivable(new BigDecimal("2500.00"));
        PrintContext.StatementItem si = new PrintContext.StatementItem();
        si.setDate(new Date());
        si.setNo("XS202610050001");
        si.setContent("销售");
        si.setProductName("料管");
        si.setQty(BigDecimal.TEN);
        si.setQtyText("10");
        si.setPrice(new BigDecimal("50.00"));
        si.setAmount(new BigDecimal("500.00"));
        si.setSettled(BigDecimal.ZERO);
        si.setUnsettled(new BigDecimal("500.00"));
        statement.setItems(List.of(si));
        ctx.setStatement(statement);
        // 成本利润示例同样无条件填充，避免模板直接引用 ${profit.*} 时预览报 null
        PrintContext.Profit profit = new PrintContext.Profit();
        profit.setCostAmount(new BigDecimal("158.80"));
        profit.setGrossProfit(new BigDecimal("341.20"));
        profit.setGrossMargin(new BigDecimal("68.24"));
        ctx.setProfit(profit);
        ctx.setShowProfit(showProfit);
        ctx.setPrintDate(LocalDate.now().format(DATE_FMT));
        return ctx;
    }

    private void fillPartner(PrintContext ctx, String partyType, Long partyId) {
        if (partyId == null) {
            return;
        }
        if ("AR".equals(partyType)) {
            ErpCustomer c = customerMapper.selectById(partyId);
            if (c != null) {
                ctx.getPartner().setName(c.getName());
                ctx.getPartner().setContact(c.getContact());
                ctx.getPartner().setPhone(c.getPhone());
                ctx.getPartner().setAddress(c.getAddress());
            }
        } else {
            ErpSupplier s = supplierMapper.selectById(partyId);
            if (s != null) {
                ctx.getPartner().setName(s.getName());
                ctx.getPartner().setContact(s.getContact());
                ctx.getPartner().setPhone(s.getPhone());
                ctx.getPartner().setAddress(s.getAddress());
            }
        }
    }

    private List<PrintContext.Item> loadAllocSourceNos(List<? extends Object> allocs) {
        List<PrintContext.Item> items = new ArrayList<>();
        for (Object o : allocs) {
            Long arApId;
            BigDecimal amount;
            if (o instanceof FinReceiptAlloc alloc) {
                arApId = alloc.getArApId();
                amount = alloc.getAmount();
            } else if (o instanceof FinPaymentAlloc alloc) {
                arApId = alloc.getArApId();
                amount = alloc.getAmount();
            } else {
                continue;
            }
            FinArAp arAp = arApMapper.selectById(arApId);
            PrintContext.Item item = new PrintContext.Item();
            item.setNo(arAp == null ? null : resolveSourceNo(arAp));
            item.setDate(arAp == null ? null : arAp.getBizDate());
            item.setAmount(amount);
            items.add(item);
        }
        return items;
    }

    private String resolveSourceNo(FinArAp arAp) {
        return switch (arAp.getSourceType() == null ? "" : arAp.getSourceType()) {
            case "SALE" -> {
                ErpSale sale = saleMapper.selectById(arAp.getSourceId());
                yield sale == null ? null : sale.getNo();
            }
            case "PURCHASE" -> {
                ErpPurchase purchase = purchaseMapper.selectById(arAp.getSourceId());
                yield purchase == null ? null : purchase.getNo();
            }
            default -> arAp.getSourceType();
        };
    }

    private PrintContext.StatementItem statementItem(Date date, String no, String content,
                                                     String productName, BigDecimal amount, BigDecimal qty) {
        PrintContext.StatementItem si = new PrintContext.StatementItem();
        si.setDate(date);
        si.setNo(no);
        si.setContent(content);
        si.setProductName(productName);
        si.setQty(qty);
        si.setAmount(amount);
        si.setSettled(BigDecimal.ZERO);
        si.setUnsettled(amount);
        return si;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private List<ErpSaleVoRow> loadSaleItems(Long saleId) {
        List<ErpSaleVoRow> rows = new ArrayList<>();
        for (ErpSaleItem item : saleItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleItem>()
                    .eq(ErpSaleItem::getOrderId, saleId)
                    .orderByAsc(ErpSaleItem::getSeq))) {
            ErpSaleVoRow row = new ErpSaleVoRow();
            ErpProduct product = item.getProductId() == null ? null : productMapper.selectById(item.getProductId());
            row.name = product == null ? null : product.getName();
            row.spec = product == null ? null : product.getSpec();
            row.unit = product == null ? null : product.getUnit();
            row.qty = item.getQty();
            row.price = item.getPrice();
            row.amount = item.getAmount();
            row.taxType = item.getTaxType();
            row.remark = item.getRemark();
            rows.add(row);
        }
        return rows;
    }

    private static class ErpSaleVoRow {
        String name;
        String spec;
        String unit;
        BigDecimal qty;
        BigDecimal price;
        BigDecimal amount;
        String taxType;
        String remark;
    }
}
