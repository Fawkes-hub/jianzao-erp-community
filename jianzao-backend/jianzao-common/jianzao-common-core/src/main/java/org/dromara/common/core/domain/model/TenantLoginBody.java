package org.dromara.common.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 多企业二次登录对象
 * <p>
 * 多企业首次登录只返回 ticket + 企业列表，不签发业务 token；
 * 前端选择企业后携带 ticket 与 tenantId 换取 token。
 *
 * @author Fawkes
 */
@Data
public class TenantLoginBody implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 一次性登录凭证（登录阶段签发，5 分钟有效，消费后作废）
     */
    @NotBlank(message = "登录凭证不能为空")
    private String ticket;

    /**
     * 选择进入的企业ID（必须在该凭证允许的企业列表内）
     */
    @NotBlank(message = "企业不能为空")
    private String tenantId;

    /**
     * 客户端ID（必须与签发凭证时的客户端一致，防止票据被复制到其他客户端使用）
     */
    @NotBlank(message = "客户端不能为空")
    private String clientId;

}
