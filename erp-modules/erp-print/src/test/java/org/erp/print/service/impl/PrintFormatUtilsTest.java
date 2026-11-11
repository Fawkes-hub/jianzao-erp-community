package org.erp.print.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 打印格式化工具测试（金额两位/数量三位/人民币大写）
 */
@Tag("dev")
class PrintFormatUtilsTest {

    @Test
    @DisplayName("money 两位小数")
    void money() {
        assertEquals("500.00", PrintFormatUtils.money(new BigDecimal("500")));
        assertEquals("10.67", PrintFormatUtils.money(new BigDecimal("10.6666")));
        assertEquals("0.00", PrintFormatUtils.money(null));
        assertEquals("0.00", PrintFormatUtils.money(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("qty 三位小数去尾零")
    void qty() {
        assertEquals("10", PrintFormatUtils.qty(new BigDecimal("10.000")));
        assertEquals("10.667", PrintFormatUtils.qty(new BigDecimal("10.6666")));
        assertEquals("0.000", PrintFormatUtils.qty(null));
    }

    @Test
    @DisplayName("大写：零/整/基本数")
    void upperBasics() {
        assertEquals("零元整", PrintFormatUtils.upper(null));
        assertEquals("零元整", PrintFormatUtils.upper(BigDecimal.ZERO));
        assertEquals("壹元整", PrintFormatUtils.upper(BigDecimal.ONE));
        assertEquals("壹拾元整", PrintFormatUtils.upper(BigDecimal.TEN));
        assertEquals("壹佰元整", PrintFormatUtils.upper(new BigDecimal("100")));
        assertEquals("壹仟零壹元整", PrintFormatUtils.upper(new BigDecimal("1001")));
    }

    @Test
    @DisplayName("大写：角分")
    void upperCents() {
        assertEquals("壹拾元贰角叁分", PrintFormatUtils.upper(new BigDecimal("10.23")));
        assertEquals("壹拾元零叁分", PrintFormatUtils.upper(new BigDecimal("10.03")));
        assertEquals("壹拾元贰角整".replace("整", ""), PrintFormatUtils.upper(new BigDecimal("10.20")).replace("整", ""));
    }

    @Test
    @DisplayName("大写：万级与亿级")
    void upperLarge() {
        assertEquals("壹万零壹元整", PrintFormatUtils.upper(new BigDecimal("10001")));
        assertEquals("壹拾万零壹佰元整", PrintFormatUtils.upper(new BigDecimal("100100")));
        assertEquals("壹亿零壹元整", PrintFormatUtils.upper(new BigDecimal("100000001")));
        assertEquals("壹亿贰仟叁佰肆拾伍万陆仟柒佰捌拾玖元玖角壹分",
            PrintFormatUtils.upper(new BigDecimal("123456789.91")));
    }

    @Test
    @DisplayName("大写：负数")
    void upperNegative() {
        assertEquals("负伍元整", PrintFormatUtils.upper(new BigDecimal("-5")));
    }

    @Test
    @DisplayName("大写：连续零合并（10101010）")
    void upperZeroMerge() {
        assertEquals("壹仟零壹拾万壹仟零壹拾元整", PrintFormatUtils.upper(new BigDecimal("10101010")));
    }
}
