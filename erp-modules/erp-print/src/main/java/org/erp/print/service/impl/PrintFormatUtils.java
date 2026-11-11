package org.erp.print.service.impl;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 打印格式化工具（DOC/14 §4：money 两位千分位、qty 三位、人民币大写）
 */
public final class PrintFormatUtils {

    private static final String[] UPPER_DIGITS = {
        "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"
    };
    private static final String[] UPPER_UNITS = {"", "拾", "佰", "仟"};
    private static final String[] UPPER_SECTIONS = {"", "万", "亿", "万亿"};

    private PrintFormatUtils() {
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public static String qty(BigDecimal value) {
        if (value == null) {
            return "0.000";
        }
        return value.setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * 人民币大写（支持到万亿；负数加"负"字；零元整）。
     */
    public static String upper(BigDecimal value) {
        if (value == null || value.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "零元整";
        }
        boolean negative = value.compareTo(java.math.BigDecimal.ZERO) < 0;
        java.math.BigDecimal abs = value.abs().setScale(2, java.math.RoundingMode.DOWN);
        long yuan = abs.longValue();
        int cents = abs.subtract(java.math.BigDecimal.valueOf(yuan))
            .multiply(java.math.BigDecimal.valueOf(100)).intValue();

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append("负");
        }
        sb.append(upperInteger(yuan)).append("元");
        if (cents == 0) {
            sb.append("整");
        } else {
            int jiao = cents / 10;
            int fen = cents % 10;
            if (jiao > 0) {
                sb.append(UPPER_DIGITS[jiao]).append("角");
            } else if (fen > 0) {
                sb.append("零");
            }
            if (fen > 0) {
                sb.append(UPPER_DIGITS[fen]).append("分");
            }
        }
        return sb.toString();
    }

    private static String upperInteger(long number) {
        if (number == 0) {
            return "零";
        }
        Deque<Integer> stack = new ArrayDeque<>();
        while (number > 0) {
            stack.push((int) (number % 10000));
            number /= 10000;
        }
        List<Integer> sections = new ArrayList<>(stack);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sections.size(); i++) {
            int section = sections.get(i);
            int secIdx = sections.size() - 1 - i;
            if (section == 0) {
                if (sb.length() > 0 && hasNonzeroAfter(sections, i)
                        && !sb.toString().endsWith("零")) {
                    sb.append("零");
                }
                continue;
            }
            if (i > 0 && section < 1000 && !sb.toString().endsWith("零")) {
                sb.append("零");
            }
            sb.append(sectionText(section)).append(UPPER_SECTIONS[secIdx]);
        }
        return sb.toString();
    }

    private static boolean hasNonzeroAfter(List<Integer> sections, int index) {
        for (int i = index + 1; i < sections.size(); i++) {
            if (sections.get(i) != 0) {
                return true;
            }
        }
        return false;
    }

    private static String sectionText(int section) {
        StringBuilder sb = new StringBuilder();
        boolean printed = false;
        boolean pendingZero = false;
        for (int d = 1000; d >= 1; d /= 10) {
            int digit = (section / d) % 10;
            if (digit == 0) {
                if (printed) {
                    pendingZero = true;
                }
            } else {
                if (pendingZero) {
                    sb.append("零");
                    pendingZero = false;
                }
                sb.append(UPPER_DIGITS[digit]).append(
                    d == 1000 ? UPPER_UNITS[3] : d == 100 ? UPPER_UNITS[2] : d == 10 ? UPPER_UNITS[1] : UPPER_UNITS[0]);
                printed = true;
            }
        }
        return sb.toString();
    }
}
