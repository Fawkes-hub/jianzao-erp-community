package org.dromara.common.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 企业自主注册对象
 * <p>
 * 企业名 + 手机号 + 密码 + 滑块票据；手机号同时作为管理员账号，注册成功后自动登录。
 *
 * @author Fawkes
 */
@Data
public class TenantRegisterBody implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 企业名称（注册后作为租户 company_name 与默认部门名）
     */
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 30, message = "企业名称长度不能超过{max}个字符")
    private String companyName;

    /**
     * 登录手机号（同时作为管理员账号）
     */
    @NotBlank(message = "{user.phonenumber.not.blank}")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "{user.mobile.phone.number.not.valid}")
    private String phone;

    /**
     * 初始密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    @Length(min = 5, max = 30, message = "{user.password.length.valid}")
    private String password;

    /**
     * 滑块校验通过后服务端签发的一次性票据
     */
    private String code;

    /**
     * 滑块票据唯一标识
     */
    private String uuid;

}
