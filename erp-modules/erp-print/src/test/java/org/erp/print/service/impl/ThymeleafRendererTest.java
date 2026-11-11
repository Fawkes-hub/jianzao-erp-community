package org.erp.print.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Thymeleaf 渲染引擎测试：转义 / 循环 / 条件 / 变量注入
 */
@Tag("dev")
class ThymeleafRendererTest {

    @Test
    @DisplayName("基本变量替换")
    void renderVariables() {
        String html = ThymeleafRenderer.render(
            "<p th:text=\"${doc.no}\">x</p>",
            Map.of("doc", Map.of("no", "XS001")));
        assertTrue(html.contains("XS001"));
    }

    @Test
    @DisplayName("默认转义防 XSS")
    void escapesHtml() {
        String html = ThymeleafRenderer.render(
            "<p th:text=\"${partner.name}\">x</p>",
            Map.of("partner", Map.of("name", "<script>alert(1)</script>")));
        assertEquals(-1, html.indexOf("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    @DisplayName("循环渲染明细")
    void rendersLoop() {
        String tpl = "<tr th:each=\"i : ${items}\"><td th:text=\"${i.no}\">n</td></tr>";
        String html = ThymeleafRenderer.render(tpl, Map.of("items",
            java.util.List.of(Map.of("no", "A"), Map.of("no", "B"))));
        assertTrue(html.contains("A"));
        assertTrue(html.contains("B"));
    }

    @Test
    @DisplayName("条件渲染 showProfit")
    void rendersConditional() {
        String tpl = "<div th:if=\"${showProfit}\">利润</div>";
        assertTrue(ThymeleafRenderer.render(tpl, Map.of("showProfit", true)).contains("利润"));
        assertEquals(-1, ThymeleafRenderer.render(tpl, Map.of("showProfit", false)).indexOf("利润"));
    }

    @Test
    @DisplayName("th:utext 不被白名单允许但语法仍可执行——确保我们不用它（文档约定靠模板审查）")
    void utextStillEscapedByOurUsage() {
        // 本引擎只提供能力，模板来源是 jsoup 清洗后的内容 + 内置模板；
        // 此用例固化行为：th:text 是转义输出
        String html = ThymeleafRenderer.render(
            "<span th:text=\"${x}\">y</span>",
            Map.of("x", "<b>bold</b>"));
        assertTrue(html.contains("&lt;b&gt;"));
    }

    @Test
    @DisplayName("语法错误抛异常")
    void syntaxErrorThrows() {
        assertThrows(Exception.class,
            () -> ThymeleafRenderer.render("<p th:text=\"${\">x</p>", Map.of()));
    }

    @Test
    @DisplayName("联单复制：copies<=1 原样返回")
    void copiesOneUnchanged() {
        String html = "<html><head><style>p{color:red}</style></head><body><p>单据</p></body></html>";
        assertEquals(html, ThymeleafRenderer.renderWithCopies(html, 1));
        assertEquals(html, ThymeleafRenderer.renderWithCopies(html, 0));
    }

    @Test
    @DisplayName("联单复制：3 份三联，份间分页符，head 样式只保留一份")
    void copiesThreeWrapsWithPageBreaks() {
        String html = "<html><head><style>table{width:100%}</style></head>"
            + "<body><table><tr><td>货</td></tr></table></body></html>";
        String out = ThymeleafRenderer.renderWithCopies(html, 3);
        assertEquals(3, count(out, "class=\"print-copy\""));
        // 末份不插分页符，避免多打一页空白
        assertEquals(2, count(out, "page-break-after:always"));
        assertEquals(1, count(out, "table{width:100%}"));
        assertEquals(3, count(out, "货"));
        assertTrue(out.startsWith("<!DOCTYPE html>"));
    }

    private static int count(String hay, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = hay.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }
}
