package org.erp.master.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import org.dromara.common.core.exception.ServiceException;

import java.text.Normalizer;
import java.util.function.Predicate;

/**
 * 主数据编码生成：名称取拼音首字母，重复时追加上序号。
 */
public final class CodeGenerator {

    private static final int MAX_LENGTH = 32;
    private static final int MAX_SEQ = 9999;

    private CodeGenerator() {
    }

    public static String generate(String name, String fallbackPrefix, Predicate<String> exists) {
        String base = baseCode(name, fallbackPrefix);
        if (!exists.test(base)) {
            return base;
        }
        for (int seq = 2; seq <= MAX_SEQ; seq++) {
            String suffix = "-" + seq;
            String candidate = base.substring(0, Math.min(base.length(), MAX_LENGTH - suffix.length())) + suffix;
            if (!exists.test(candidate)) {
                return candidate;
            }
        }
        throw new ServiceException("编码自动生成失败，请手动填写编码", 40903);
    }

    static String baseCode(String name, String fallbackPrefix) {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC);
            for (int i = 0; i < normalized.length() && sb.length() < MAX_LENGTH; i++) {
                char c = normalized.charAt(i);
                if (c >= 'a' && c <= 'z') {
                    sb.append(Character.toUpperCase(c));
                } else if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                    sb.append(c);
                } else if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c);
                    if (pinyins != null && pinyins.length > 0 && !pinyins[0].isEmpty()) {
                        sb.append(Character.toUpperCase(pinyins[0].charAt(0)));
                    }
                }
            }
        }
        if (sb.length() == 0) {
            sb.append(fallbackPrefix);
        }
        return sb.substring(0, Math.min(sb.length(), MAX_LENGTH));
    }
}
