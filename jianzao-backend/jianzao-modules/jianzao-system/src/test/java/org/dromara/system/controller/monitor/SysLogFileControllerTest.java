package org.dromara.system.controller.monitor;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysLogFileController 单元测试（不依赖 Web 容器，直接验证白名单与 tail 逻辑）
 */
@Tag("dev")
class SysLogFileControllerTest {

    @TempDir
    Path tempDir;

    private SysLogFileController newController(Path logDir) throws Exception {
        SysLogFileController controller = new SysLogFileController();
        java.lang.reflect.Field field = SysLogFileController.class.getDeclaredField("logDir");
        field.setAccessible(true);
        field.set(controller, logDir.toFile());
        return controller;
    }

    @Test
    void tailReturnsLastLines() throws Exception {
        Path log = tempDir.resolve("sys-error.log");
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 1000; i++) {
            content.append("error line ").append(i).append('\n');
        }
        Files.writeString(log, content);
        SysLogFileController controller = newController(tempDir);

        Map<String, String> result = controller.tail("sys-error.log", 10).getData();
        String[] lines = result.get("content").split("\n");
        assertEquals(10, lines.length);
        assertEquals("error line 991", lines[0]);
        assertEquals("error line 1000", lines[9]);
    }

    @Test
    void tailMultibyteUtf8NotCorrupted() throws Exception {
        Path log = tempDir.resolve("sys-info.log");
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 200; i++) {
            content.append("中文日志内容：保存客户失败-").append(i).append('\n');
        }
        Files.writeString(log, content);
        SysLogFileController controller = newController(tempDir);

        String result = controller.tail("sys-info.log", 5).getData().get("content");
        assertTrue(result.contains("保存客户失败-200"));
        assertFalse(result.contains("�"), "utf-8 多字节字符不应被截断成乱码");
    }

    @Test
    void tailMissingFileReturnsEmpty() throws Exception {
        SysLogFileController controller = newController(tempDir);
        assertEquals("", controller.tail("sys-error.log", 10).getData().get("content"));
    }

    @Test
    void tailRejectsUnknownFile() throws Exception {
        SysLogFileController controller = newController(tempDir);
        org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
            () -> controller.tail("../../etc/passwd", 10));
        org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
            () -> controller.tail("sys-info.log.gz", 10));
    }

    @Test
    void filesListsWhitelistInOrder() throws Exception {
        SysLogFileController controller = newController(tempDir);
        List<SysLogFileController.SysLogFileVo> files = controller.files().getData();
        assertEquals(3, files.size());
        assertEquals("sys-error.log", files.get(0).getName());
        assertEquals("错误日志", files.get(0).getLabel());
        assertFalse(files.get(0).getExists());
    }

    @Test
    void downloadOnlyAllowsWhitelist() throws Exception {
        SysLogFileController controller = newController(tempDir);
        Files.writeString(tempDir.resolve("sys-error.log"), "boom\n");
        // 白名单内文件存在即可被 tail 读到
        assertTrue(controller.tail("sys-error.log", 5).getData().get("content").contains("boom"));
        // resolveAllowed 对非法名抛异常
        org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
            () -> controller.download("../../secret", null));
    }
}
