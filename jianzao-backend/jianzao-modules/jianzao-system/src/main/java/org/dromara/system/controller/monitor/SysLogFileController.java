package org.dromara.system.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统运行日志文件查看（只读）
 *
 * <p>仅允许读取 logback 输出目录下的白名单文件，路径不接收用户输入的目录部分。</p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/syslog")
public class SysLogFileController extends BaseController {

    /**
     * 白名单：文件名 -> 展示名。log.path 固定为 ./logs（见 logback-plus.xml）。
     */
    private static final Map<String, String> ALLOWED_FILES;
    static {
        // 错误日志最常看，排第一
        Map<String, String> files = new LinkedHashMap<>();
        files.put("sys-error.log", "错误日志");
        files.put("sys-info.log", "信息日志");
        files.put("sys-console.log", "控制台日志");
        ALLOWED_FILES = java.util.Collections.unmodifiableMap(files);
    }

    private final File logDir = new File("logs").getAbsoluteFile();

    private static final int MAX_TAIL_LINES = 5000;
    private static final long MAX_TAIL_BYTES = 4L * 1024 * 1024;

    /**
     * 可查看的日志文件列表（含存在性与大小）
     */
    @SaCheckPermission("monitor:syslog:list")
    @GetMapping("/files")
    public R<List<SysLogFileVo>> files() {
        List<SysLogFileVo> list = new ArrayList<>();
        ALLOWED_FILES.forEach((name, label) -> {
            SysLogFileVo vo = new SysLogFileVo();
            vo.setName(name);
            vo.setLabel(label);
            File file = new File(logDir, name);
            vo.setExists(file.isFile());
            vo.setSizeBytes(file.isFile() ? file.length() : 0L);
            list.add(vo);
        });
        return R.ok(list);
    }

    /**
     * 读取日志尾部内容
     *
     * @param name  白名单内的日志文件名
     * @param lines 尾部行数，默认 500，最大 5000
     */
    @SaCheckPermission("monitor:syslog:list")
    @GetMapping("/tail")
    public R<Map<String, String>> tail(@RequestParam String name,
                                       @RequestParam(defaultValue = "500") int lines) {
        String fileName = resolveAllowed(name);
        if (lines < 1) {
            lines = 1;
        }
        lines = Math.min(lines, MAX_TAIL_LINES);
        File file = new File(logDir, fileName);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("name", fileName);
        result.put("content", file.isFile() ? tail(file, lines) : "");
        return R.ok(result);
    }

    /**
     * 下载日志文件（仅白名单）
     */
    @SaCheckPermission("monitor:syslog:list")
    @GetMapping("/download")
    public void download(@RequestParam String name, HttpServletResponse response) throws IOException {
        String fileName = resolveAllowed(name);
        File file = new File(logDir, fileName);
        if (!file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"");
        response.setContentLengthLong(file.length());
        java.nio.file.Files.copy(file.toPath(), response.getOutputStream());
        response.getOutputStream().flush();
    }

    private String resolveAllowed(String name) {
        if (name == null || !ALLOWED_FILES.containsKey(name)) {
            throw new ServiceException("日志文件不在允许列表内");
        }
        return name;
    }

    /**
     * 从文件末尾向前扫描，取最后 lines 行；单次最多回读 MAX_TAIL_BYTES，超长行截断
     */
    private String tail(File file, int lines) {
        long length = file.length();
        int readSize = (int) Math.min(length, MAX_TAIL_BYTES);
        byte[] buffer = new byte[readSize];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(length - readSize);
            raf.readFully(buffer);
        } catch (IOException e) {
            return "";
        }
        String text = new String(buffer, StandardCharsets.UTF_8);
        // 去掉可能被截断的首行
        int firstLineEnd = text.indexOf('\n');
        if (readSize < length && firstLineEnd >= 0) {
            text = text.substring(firstLineEnd + 1);
        }
        String[] split = text.split("\n", -1);
        // 末尾如有换行，split 会多出空串，去掉
        int end = split.length > 0 && split[split.length - 1].isEmpty() ? split.length - 1 : split.length;
        int from = Math.max(0, end - lines);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < end; i++) {
            sb.append(split[i]).append('\n');
        }
        return sb.toString();
    }

    @Data
    public static class SysLogFileVo {
        private String name;
        private String label;
        private Boolean exists;
        private Long sizeBytes;
    }
}
