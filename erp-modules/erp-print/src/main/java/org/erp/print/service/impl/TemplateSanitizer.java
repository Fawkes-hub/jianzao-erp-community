package org.erp.print.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板白名单清洗（DOC/14 §3/§9）
 *
 * 只允许打印版式所需标签与样式属性；去掉 script/事件属性/javascript: 协议/外部 iframe。
 * Saferelative 允许相对协议但不允许外链绝对 URL 的资源加载需求由禁 img src 外链覆盖。
 * th:utext/th:attr/th:href/th:src 会在渲染期生成未转义 HTML、事件属性或动态协议，白名单直接拒绝。
 */
@Component
public class TemplateSanitizer {

    /**
     * 渲染期可生成未转义 HTML 或动态链接的指令，存在 XSS 风险，模板保存时直接拒绝
     */
    private static final String[] FORBIDDEN_DIRECTIVES = {"th:utext", "th:attr", "th:href", "th:src"};

    private final Safelist safelist;

    public TemplateSanitizer() {
        this.safelist = new Safelist()
            .addTags("html", "head", "body", "title", "meta", "style", "div", "span", "p", "br", "hr",
                "h1", "h2", "h3", "h4", "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                "ul", "ol", "li", "b", "strong", "i", "em", "u", "small", "sub", "sup",
                "img", "section", "header", "footer", "font")
            .addAttributes(":all", "style", "class", "width", "height", "align", "valign",
                "colspan", "rowspan", "border", "cellpadding", "cellspacing", "title")
            // Thymeleaf 动态绑定指令：不清洗则模板保存后动态内容全部失效；仅保留转义输出与结构控制
            .addAttributes(":all", "th:text", "th:if", "th:unless", "th:each", "th:with",
                "th:value", "th:class", "th:style",
                "th:switch", "th:case", "th:insert", "th:replace", "th:block")
            .addAttributes("table", "frame", "rules")
            .addAttributes("meta", "charset")
            .addAttributes("img", "src", "alt")
            .addProtocols("img", "src", "data")
            .addEnforcedAttribute("meta", "http-equiv", "Content-Type");
    }

    /**
     * 清洗并校验：清洗后若文档仍含 script 标签、on* 事件属性或 javascript: 协议则拒绝。
     *
     * @param html 模板 HTML 源码
     * @return 清洗后的 HTML
     */
    public String clean(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("模板内容不能为空");
        }
        // 显式拒绝渲染期可生成未转义 HTML/事件属性/动态协议的指令，避免 Jsoup 静默剥离造成模板悄悄失效
        String lowerHtml = html.toLowerCase(Locale.ROOT);
        for (String directive : FORBIDDEN_DIRECTIVES) {
            if (lowerHtml.contains(directive)) {
                throw new IllegalArgumentException("模板禁止使用 " + directive + "：可能渲染未转义 HTML、事件属性或动态链接");
            }
        }
        // 纯文本兜底：上游（如全局 XSS 过滤器）剥离全部标签后只剩外层 body 与文字，
        // 存库会变成渲染空壳模板，故直接拒绝，避免静默产生坏数据
        Matcher tagMatcher = Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)[\\s/>]").matcher(html);
        boolean hasRealTag = false;
        while (tagMatcher.find()) {
            String tag = tagMatcher.group(1).toLowerCase();
            if (!"body".equals(tag) && !"html".equals(tag) && !"head".equals(tag)) {
                hasRealTag = true;
                break;
            }
        }
        if (!hasRealTag) {
            throw new IllegalArgumentException("模板内容不含有效 HTML 标签，疑似被转成纯文本，请粘贴模板源码");
        }
        String cleaned = Jsoup.clean(html, this.safelist);
        Document doc = Jsoup.parse(cleaned);
        if (!doc.select("script, iframe, object, embed, form, link").isEmpty()) {
            throw new IllegalArgumentException("模板禁止包含 script/iframe/object/embed/form/link 标签");
        }
        for (Element el : doc.select("*")) {
            for (var attr : el.attributes()) {
                String key = attr.getKey().toLowerCase();
                String value = attr.getValue() == null ? "" : attr.getValue().toLowerCase();
                if (key.startsWith("on")) {
                    throw new IllegalArgumentException("模板禁止包含事件属性：" + key);
                }
                if (value.contains("javascript:") || value.contains("expression(")) {
                    throw new IllegalArgumentException("模板禁止包含 javascript: 或 CSS expression");
                }
            }
        }
        // 外链检查：src/href 只允许 data: 与文档内锚点
        for (Element el : doc.select("[src],[href]")) {
            String url = el.hasAttr("src") ? el.attr("src") : el.attr("href");
            String v = url.toLowerCase();
            if (v.startsWith("http:") || v.startsWith("https:") || v.startsWith("//")) {
                throw new IllegalArgumentException("模板禁止外部资源链接：" + url);
            }
        }
        return cleaned;
    }
}
