package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 首页
 *
 * @author Lion Li
 */
@SaIgnore
@RequiredArgsConstructor
@RestController
public class IndexController {

    /**
     * 本地单机模式由后端直接托管前端页面时，转发到 app/dist/index.html；
     * 服务器部署无该目录，保持原有提示语行为。
     */
    private static final Path LOCAL_SPA_INDEX = Paths.get("app", "dist", "index.html");

    /**
     * 访问首页
     */
    @GetMapping("/")
    public void index(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (Files.exists(LOCAL_SPA_INDEX)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(StringUtils.format("欢迎使用{}后台管理框架，请通过前端地址访问。", SpringUtils.getApplicationName()));
    }

}
