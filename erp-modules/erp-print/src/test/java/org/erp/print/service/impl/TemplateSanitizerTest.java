package org.erp.print.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模板白名单清洗测试
 */
@Tag("dev")
class TemplateSanitizerTest {

    private final TemplateSanitizer sanitizer = new TemplateSanitizer();

    @Test
    @DisplayName("正常模板保留")
    void keepNormalTemplate() {
        String html = """
            <html><head><style>@page { size: A4; }</style></head>
            <body><h1>送货单</h1><table><tr><td class="num" style="text-align:right">100</td></tr></table></body>
            </html>
            """;
        String cleaned = sanitizer.clean(html);
        assertTrue(cleaned.contains("送货单"));
        assertTrue(cleaned.contains("@page"));
    }

    @Test
    @DisplayName("保留 Thymeleaf 动态绑定指令")
    void keepThymeleafAttributes() {
        String html = """
            <html><body><h1 th:text="${doc.no}"></h1>
            <table><tr th:each="i : ${items}"><td th:text="${i.name}"></td></tr></table>
            <div th:if="${showProfit}" th:class="profit">利润</div></body></html>
            """;
        String cleaned = sanitizer.clean(html);
        assertTrue(cleaned.contains("th:text"), "th:text 应保留");
        assertTrue(cleaned.contains("th:each"), "th:each 应保留");
        assertTrue(cleaned.contains("th:if"), "th:if 应保留");
        assertTrue(cleaned.contains("th:class"), "th:class 应保留");
        assertTrue(cleaned.contains("${doc.no}"), "模板变量表达式应保留");
    }

    @Test
    @DisplayName("移除 script 标签")
    void removeScript() {
        String cleaned = sanitizer.clean("<div>ok</div><script>alert(1)</script>");
        assertEquals(-1, cleaned.toLowerCase().indexOf("<script"));
        assertEquals(-1, cleaned.indexOf("alert"));
    }

    @Test
    @DisplayName("移除 link/iframe 标签")
    void removeLinkAndIframe() {
        String cleaned = sanitizer.clean(
            "<div><link rel=\"stylesheet\" href=\"http://evil.com/x.css\">x</div><iframe src=\"http://evil.com\"></iframe>");
        assertEquals(-1, cleaned.toLowerCase().indexOf("<link"));
        assertEquals(-1, cleaned.toLowerCase().indexOf("<iframe"));
    }

    @Test
    @DisplayName("移除事件属性")
    void removeEventAttributes() {
        String cleaned = sanitizer.clean("<div onclick=\"alert(1)\">ok</div>");
        assertEquals(-1, cleaned.indexOf("onclick"));
    }

    @Test
    @DisplayName("移除外部 http 资源链接")
    void removeExternalResource() {
        assertEquals(-1, sanitizer.clean("<img src=\"http://evil.com/logo.png\">").indexOf("evil.com"));
        assertEquals(-1, sanitizer.clean("<img src=\"https://evil.com/logo.png\">").indexOf("evil.com"));
        assertEquals(-1, sanitizer.clean("<img src=\"//evil.com/logo.png\">").indexOf("evil.com"));
    }

    @Test
    @DisplayName("允许 data URI 图片")
    void allowDataUriImage() {
        String cleaned = sanitizer.clean("<img src=\"data:image/png;base64,iVBORw0KGgo=\">");
        assertTrue(cleaned.contains("data:image/png"));
    }

    @Test
    @DisplayName("移除 javascript: 协议")
    void removeJavascriptProtocol() {
        assertEquals(-1, sanitizer.clean("<a href=\"javascript:alert(1)\">x</a>").indexOf("javascript"));
    }

    @Test
    @DisplayName("空内容拒绝")
    void rejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> sanitizer.clean("  "));
        assertThrows(IllegalArgumentException.class, () -> sanitizer.clean(null));
    }

    @Test
    @DisplayName("纯文本模板拒绝（全局 XSS 过滤器剥离标签后入库的形态）")
    void rejectPlainText() {
        String flattened = """
            <body>
             @page { size: A4; margin: 12mm; } body { font-family: sans-serif; } 对 账 单 2026-10 月度
             日期 单号 商品名称 数量 单价 金额 已收 未结 2026-10-05 XS202610050001 料管 10.000 0.00
            </body>
            """;
        assertThrows(IllegalArgumentException.class, () -> sanitizer.clean(flattened));
    }

    @Test
    @DisplayName("拒绝 th:utext，防止渲染未转义 HTML")
    void rejectUtext() {
        assertThrows(IllegalArgumentException.class,
            () -> sanitizer.clean("<div th:utext=\"${remark}\">x</div>"));
        assertThrows(IllegalArgumentException.class,
            () -> sanitizer.clean("<div TH:UTEXT=\"${remark}\">x</div>"));
    }

    @Test
    @DisplayName("拒绝 th:attr/th:href/th:src，防止动态事件属性与 javascript: 链接")
    void rejectDynamicAttributes() {
        assertThrows(IllegalArgumentException.class,
            () -> sanitizer.clean("<div th:attr=\"onclick=${code}\">x</div>"));
        assertThrows(IllegalArgumentException.class,
            () -> sanitizer.clean("<a th:href=\"${url}\">x</a>"));
        assertThrows(IllegalArgumentException.class,
            () -> sanitizer.clean("<img th:src=\"${url}\">"));
    }
}
