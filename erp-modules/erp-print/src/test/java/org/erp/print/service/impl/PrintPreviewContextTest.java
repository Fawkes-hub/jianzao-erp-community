package org.erp.print.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模板预览测试：预览必须无条件填充全部示例变量，
 * 允许「场景与模板内容不一致」（如送货单场景套用对账单模板）时不抛 OGNL null 异常。
 */
@Tag("dev")
class PrintPreviewContextTest {

    @Test
    @DisplayName("非对账场景预览对账模板不报错")
    void previewStatementTemplateUnderDeliveryScene() {
        // preview 只用示例上下文渲染，不访问数据库；mapper 依赖传 null 即可
        PrintServiceImpl service = new PrintServiceImpl(null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        String tpl = """
            <div th:text="${statement.month}">2026-11</div>
            <table><tr th:each="item : ${statement.items}">
              <td th:text="${item.no}">XS1</td>
              <td th:text="${item.price}">0.00</td>
            </tr></table>
            <div th:text="${profit.grossProfit}">0.00</div>
            """;
        String html = service.preview("SALE_DELIVERY", tpl, null, false, "A4");
        assertTrue(html.contains("XS202610050001"), "应渲染示例对账明细");
        assertTrue(html.contains("50.00"), "应渲染示例单价");
        assertTrue(html.contains("341.20"), "应渲染示例毛利");
    }

    @Test
    @DisplayName("预览按 paper 注入 @page：针式纸声明与实际装纸一致")
    void previewInjectsPageRuleByPaper() {
        PrintServiceImpl service = new PrintServiceImpl(null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        String html = service.preview("SALE_DELIVERY", "<p>x</p>", null, false, "241x140");
        assertTrue(html.contains("@page { size: 241mm 140mm"), "预览 HTML 应包含针式纸 @page 规则");
    }

    @Test
    @DisplayName("模板自带 @page 时预览不覆盖：精调边距保持原样")
    void previewKeepsOwnPageRule() {
        PrintServiceImpl service = new PrintServiceImpl(null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        String html = service.preview("SALE_DELIVERY", "<style>@page { size: 241mm 140mm; margin: 5mm 10mm; }</style><p>x</p>",
            null, false, "A4");
        assertTrue(html.contains("margin: 5mm 10mm"), "模板精调边距应保持原样");
        assertEquals(-1, html.indexOf("margin: 12mm"), "不应注入 A4 默认边距");
    }
}
