package org.erp.print.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内置对账单模板渲染测试：验证「内容」列已替换为「单价」列，且单价按销售明细行输出。
 */
@Tag("dev")
class BuiltinStatementTemplateTest {

    @Test
    @DisplayName("内置对账单渲染单价列")
    void rendersUnitPriceColumn() {
        PrintContext.StatementItem sale = new PrintContext.StatementItem();
        sale.setDate(new Date());
        sale.setNo("XS202611050001");
        sale.setContent("销售");
        sale.setProductName("汤勺铝芯");
        sale.setQty(new BigDecimal("10"));
        sale.setQtyText("10");
        sale.setPrice(new BigDecimal("28"));
        sale.setAmount(new BigDecimal("280.00"));
        sale.setSettled(BigDecimal.ZERO);
        sale.setUnsettled(new BigDecimal("280.00"));

        // 收款行无单价，模板应输出 '-'
        PrintContext.StatementItem receipt = new PrintContext.StatementItem();
        receipt.setDate(new Date());
        receipt.setNo("SK202611050001");
        receipt.setContent("收款");
        receipt.setAmount(new BigDecimal("100.00"));
        receipt.setSettled(BigDecimal.ZERO);
        receipt.setUnsettled(new BigDecimal("100.00"));

        PrintContext.Statement statement = new PrintContext.Statement();
        statement.setMonth("2026-11");
        statement.setOpening(BigDecimal.ZERO);
        statement.setPreviousReceivable(BigDecimal.ZERO);
        statement.setTotalReceivable(new BigDecimal("250.00"));
        statement.setSalesAmount(new BigDecimal("280.00"));
        statement.setReceiptAmount(new BigDecimal("100.00"));
        statement.setMonthlyUnpaid(new BigDecimal("180.00"));
        statement.setCumulativeSales(new BigDecimal("280.00"));
        statement.setCumulativeReceipts(new BigDecimal("100.00"));
        statement.setClosing(new BigDecimal("180.00"));
        statement.setCumulativeUnpaid(new BigDecimal("180.00"));
        statement.setItems(List.of(sale, receipt));

        PrintContext.Tenant tenant = new PrintContext.Tenant();
        tenant.setName("成都鑫盛合科技");

        Map<String, Object> model = Map.of(
            "statement", statement,
            "partner", Map.of("name", "成都冒火"),
            "tenant", tenant,
            "printDate", "2026-11-07");

        String html = ThymeleafRenderer.render(BuiltinTemplates.statement().getContent(), model);

        assertTrue(html.contains("当月销售金额：<span>280.00</span>"));
        assertTrue(html.contains("当月收款金额：<span>100.00</span>"));
        assertTrue(html.contains("当月未付金额：<span>180.00</span>"));
        assertTrue(html.contains("累计销售金额：<span>280.00</span>"));
        assertTrue(html.contains("累计未付金额：<b>180.00</b> 元"));
        assertTrue(html.contains("class=\"summary\""));
        assertTrue(html.contains("制单：<span>成都鑫盛合科技</span>"));
        assertTrue(html.contains("客户确认：______________"));
        assertTrue(html.contains("单价"), "表头应包含单价列");
        assertFalse(html.contains(">内容<"), "表头不应再包含内容列");
        assertFalse(html.contains(">销售<"), "单元格不应再输出硬编码的销售常量");
        assertTrue(html.contains("28.00"), "销售明细行单价应格式化为两位小数");
        assertTrue(html.contains(">10<"), "数量列按商品精度输出，整数商品不带小数位");
        assertFalse(html.contains("10.000"), "整数商品不应再显示三位小数");
        // 收款行 price 为空，单价单元格回落显示 '-'
        assertTrue(html.contains(">-</td>"), "无单价的行应显示 -");
    }
}
