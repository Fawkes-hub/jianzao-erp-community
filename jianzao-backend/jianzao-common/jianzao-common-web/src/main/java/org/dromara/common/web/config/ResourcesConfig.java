package org.dromara.common.web.config;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.dromara.common.web.interceptor.PlusWebInvokeTimeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Date;

/**
 * 通用配置
 *
 * @author Lion Li
 */
@AutoConfiguration
public class ResourcesConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局访问性能拦截
        registry.addInterceptor(new PlusWebInvokeTimeInterceptor());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 全局日期格式转换配置
        registry.addConverter(String.class, Date.class, source -> {
            DateTime parse = DateUtil.parse(source);
            if (ObjectUtils.isNull(parse)) {
                return null;
            }
            return parse.toJdkDate();
        });
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    }

    /**
     * 跨域配置
     * 前后端同源部署（nginx / 桌面端 127.0.0.1）时无需放开跨域；
     * 仅当 security.cors.allowed-origins 配置了明确域名时才允许跨域，禁止使用通配符。
     *
     * <p>注意：浏览器对 POST/PUT/DELETE 等非简单请求会自动携带 Origin 头，即便页面与接口同源。
     * 若此处不放行同源来源，这些写请求会被 Spring Security 判为跨域并返回 403，
     * 导致登录与所有写操作不可用。因此未配置白名单时，放行本机同源来源。</p>
     *
     * @param allowedOrigins 允许跨域的来源，多个用英文逗号分隔；为空时放行本机同源来源
     * @return 跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter(@Value("${security.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        if (StringUtils.isNotBlank(allowedOrigins)) {
            Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(StringUtils::isNotBlank).forEach(config::addAllowedOriginPattern);
        } else {
            // 未显式配置白名单：仅放行本机同源来源（HTTP/HTTPS + localhost/127.0.0.1 + 任意端口），
            // 保证 nginx 与桌面端同源部署时登录、写接口可用，同时不放开任意外域。
            for (String host : new String[]{"localhost", "127.0.0.1"}) {
                config.addAllowedOriginPattern("http://" + host + ":*");
                config.addAllowedOriginPattern("https://" + host + ":*");
                // 默认端口（80/443）时浏览器 Origin 不带端口（如 http://localhost），
                // 而 ":*" 只匹配显式端口，必须单独放行无端口形式，否则写请求仍会 403。
                config.addAllowedOriginPattern("http://" + host);
                config.addAllowedOriginPattern("https://" + host);
            }
        }
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }

    /**
     * 全局异常处理器
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
