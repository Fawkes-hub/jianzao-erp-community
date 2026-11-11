package org.erp.master.util;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CodeGenerator 单元测试
 */
@Tag("dev")
class CodeGeneratorTest {

    @Test
    void nameToPinyinInitials() {
        assertEquals("YDJX", CodeGenerator.baseCode("远达机械", "KH"));
        assertEquals("SLKJ", CodeGenerator.baseCode("示例科技", "KH"));
        assertEquals("KH2026", CodeGenerator.baseCode("客户 2026", "KH"));
        assertEquals("ABC", CodeGenerator.baseCode("abc", "KH"));
    }

    @Test
    void fallbackWhenNameHasNoCodeChars() {
        assertEquals("KH", CodeGenerator.baseCode(null, "KH"));
        assertEquals("KH", CodeGenerator.baseCode("（）", "KH"));
    }

    @Test
    void returnsBaseWhenFree() {
        assertEquals("YDJX", CodeGenerator.generate("远达机械", "KH", code -> false));
    }

    @Test
    void appendsSequenceOnDuplicate() {
        Set<String> occupied = new HashSet<>();
        occupied.add("YDJX");
        occupied.add("YDJX-2");
        assertEquals("YDJX-3", CodeGenerator.generate("远达机械", "KH", occupied::contains));
    }

    @Test
    void truncatesToMaxLength() {
        String code = CodeGenerator.baseCode("a".repeat(40), "KH");
        assertEquals(32, code.length());

        String suffixed = CodeGenerator.generate("a".repeat(40), "KH", "A".repeat(32)::equals);
        assertEquals(32, suffixed.length());
        assertTrue(suffixed.endsWith("-2"));
    }

    @Test
    void throwsWhenSequenceExhausted() {
        assertThrows(ServiceException.class, () -> CodeGenerator.generate("远达机械", "KH", code -> true));
    }
}
