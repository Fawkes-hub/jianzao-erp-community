package org.dromara.common.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 强制微信小程序实名鉴权绑定注解
 * 挂载于核心业务写入接口（如工单创建），未绑定微信号的用户禁止执行并返回 NEED_WECHAT_BIND
 *
 * @author fawkes
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireWechatBind {

    /**
     * 提示说明信息
     */
    String message() default "创建工单需先完成微信小程序实名鉴权绑定";
}
