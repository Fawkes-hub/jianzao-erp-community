package org.dromara.common.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 默认跨域白名单放行规则测试
 *
 * <p>浏览器对默认端口（80/443）会在 Origin 里省略端口，例如页面跑在 80 端口时发送
 * {@code Origin: http://localhost}。白名单若只有 {@code http://localhost:*} 会匹配不到，
 * 导致经代理改写了 Host 的写请求被判非法跨域返回 403。</p>
 *
 * @author fawkes
 */
@Tag("dev")
@DisplayName("默认跨域白名单测试")
class ResourcesConfigCorsTest {

    /**
     * 模拟 Host 已被 vite/反向代理改写成非本机域名后的跨域 POST。
     * MockHttpServletRequest 默认 serverName=localhost、port=80，与 Origin 同源会跳过校验，
     * 这里显式换成别的 Host，强制走白名单判断。
     */
    private int statusForPost(String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/code/slider");
        request.setServerName("erp.example.com");
        request.setServerPort(8080);
        request.addHeader(HttpHeaders.HOST, "erp.example.com:8080");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ResourcesConfig().corsFilter("").doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    @DisplayName("默认端口的本机来源应放行")
    void shouldAllowDefaultPortOrigins() throws Exception {
        for (String origin : new String[]{"http://localhost", "https://localhost", "http://127.0.0.1", "https://127.0.0.1"}) {
            assertEquals(200, statusForPost(origin), origin);
        }
    }

    @Test
    @DisplayName("显式端口的本机来源应放行")
    void shouldAllowExplicitPortOrigins() throws Exception {
        assertEquals(200, statusForPost("http://localhost:8081"));
        assertEquals(200, statusForPost("http://127.0.0.1:9999"));
    }

    @Test
    @DisplayName("非本机来源仍应拒绝")
    void shouldRejectForeignOrigins() throws Exception {
        assertEquals(403, statusForPost("http://evil.example"));
        assertEquals(403, statusForPost("http://localhost.evil.com"));
    }
}
