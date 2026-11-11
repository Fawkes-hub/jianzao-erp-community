package org.erp.common.excel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Excel 单元格校验与转换的公共方法。
 */
public final class CellValues {

    private CellValues() {
    }

    public static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String text(String value) {
        return value == null ? null : value.trim();
    }

    public static String required(String value, String label, int maxLength, List<String> errors) {
        String text = text(value);
        if (blank(text)) {
            errors.add(label + "不能为空");
            return null;
        }
        if (text.length() > maxLength) {
            errors.add(label + "不能超过" + maxLength + "个字符");
        }
        return text;
    }

    public static String optional(String value, String label, int maxLength, List<String> errors) {
        String text = text(value);
        if (text != null && text.length() > maxLength) {
            errors.add(label + "不能超过" + maxLength + "个字符");
        }
        return text;
    }

    public static BigDecimal decimal(String value, String label, int maxScale, boolean required,
                                     List<String> errors) {
        String text = text(value);
        if (blank(text)) {
            if (required) {
                errors.add(label + "不能为空");
                return null;
            }
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal number = new BigDecimal(text);
            if (number.stripTrailingZeros().scale() > maxScale) {
                errors.add(label + "最多保留" + maxScale + "位小数");
                return null;
            }
            return number;
        } catch (NumberFormatException e) {
            errors.add(label + "必须是数字");
            return null;
        }
    }

    public static Integer status(String value, List<String> errors) {
        String text = text(value);
        if (blank(text)) {
            return 1;
        }
        if ("启用".equals(text) || "1".equals(text)) {
            return 1;
        }
        if ("停用".equals(text) || "0".equals(text)) {
            return 0;
        }
        errors.add("状态必须是「启用」或「停用」");
        return null;
    }

    public static String choice(String value, Map<String, String> mappings, String label, boolean required,
                                List<String> errors) {
        String text = text(value);
        if (blank(text)) {
            if (required) {
                errors.add(label + "不能为空");
            }
            return null;
        }
        String mapped = mappings.get(text);
        if (mapped == null) {
            errors.add(label + "取值无效：" + text);
        }
        return mapped;
    }
}
