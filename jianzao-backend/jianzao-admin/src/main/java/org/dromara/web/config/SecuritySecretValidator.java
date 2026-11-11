package org.dromara.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 密钥启动校验
 * <p>
 * 生产与 Windows 单机包必须通过 SA_TOKEN_JWT_SECRET_KEY 注入每套部署独立的随机密钥；
 * 仓库内置默认值只允许本地开发使用，检测到默认值时打印显著告警。
 *
 * @author fawkes
 */
@Slf4j
@Component
public class SecuritySecretValidator {

    /**
     * 本地开发默认密钥（application.yml），长度满足最低要求但不得用于生产
     */
    private static final String DEV_DEFAULT_SECRET = "dev-only-jwt-secret-change-me-0123456789abcdef";

    /**
     * 构造时校验 JWT 密钥长度，避免空值或过短密钥导致 token 可被伪造
     *
     * @param jwtSecret JWT 密钥
     */
    public SecuritySecretValidator(@Value("${sa-token.jwt-secret-key:}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.trim().length() < 32) {
            throw new IllegalStateException("sa-token.jwt-secret-key 未配置或长度不足 32 位，拒绝启动");
        }
        if (DEV_DEFAULT_SECRET.equals(jwtSecret.trim())) {
            log.warn("当前使用仓库内置的开发 JWT 密钥，仅限本地调试；生产/单机包必须配置 SA_TOKEN_JWT_SECRET_KEY");
        }
    }
}
