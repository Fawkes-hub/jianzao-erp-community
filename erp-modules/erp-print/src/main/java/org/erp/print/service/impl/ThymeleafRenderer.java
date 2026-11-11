package org.erp.print.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Map;

/**
 * Thymeleaf 字符串模板渲染器（DOC/14 §3）
 *
 * 模板存 DB，用 StringTemplateResolver 每次渲染；只用 Context 放预计算数据，
 * 不暴露 SpEL/任意类调用；模板默认 th:text 转义。
 */
public final class ThymeleafRenderer {

    private static final org.thymeleaf.TemplateEngine ENGINE = new org.thymeleaf.TemplateEngine();

    static {
        org.thymeleaf.templateresolver.StringTemplateResolver resolver =
            new org.thymeleaf.templateresolver.StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        ENGINE.setTemplateResolver(resolver);
    }

    private ThymeleafRenderer() {
    }

    /**
     * 渲染模板 HTML。
     *
     * @param template 清洗后的模板 HTML
     * @param model    上下文变量根键 → 值（如 "items" → List）
     * @return 渲染后的完整 HTML
     */
    public static String render(String template, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        return ENGINE.process(new TemplateSpec(template, TemplateMode.HTML), context);
    }

    /**
     * 联单输出：同一渲染结果复制 copies 份，每份包一层 div，末份外均插入分页符；
     * head（含样式）只保留一份。copies<=1 原样返回。
     */
    public static String renderWithCopies(String renderedHtml, int copies) {
        if (copies <= 1) {
            return renderedHtml;
        }
        Document doc = Jsoup.parse(renderedHtml);
        String head = doc.head().html();
        String body = doc.body().html();
        StringBuilder sb = new StringBuilder(renderedHtml.length() * copies + 128);
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n");
        if (!head.isBlank()) {
            sb.append(head).append('\n');
        }
        sb.append("</head>\n<body>\n");
        for (int i = 1; i <= copies; i++) {
            sb.append("<div class=\"print-copy\"");
            if (i < copies) {
                sb.append(" style=\"page-break-after:always\"");
            }
            sb.append(">\n").append(body).append("\n</div>\n");
        }
        sb.append("</body>\n</html>");
        return sb.toString();
    }
}
