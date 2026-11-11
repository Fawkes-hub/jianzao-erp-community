package org.dromara.web.config;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.TACBuilder;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.common.model.dto.ResourceMap;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 行为验证码装配：只使用 tianai-captcha 的纯 Java 核心库（Java 8 编译、零 servlet 依赖），
 * 不引入官方 springboot-starter，避免其 Spring Boot 2.2.7 BOM 与 javax.servlet 污染本项目的
 * Spring Boot 3.5 / Jakarta 环境。滑动轨迹由 ImageCaptchaApplication.matching() 在服务端校验。
 * 背景图与拼图模板均由本仓 Java2D 运行时绘制，与登录页品牌蓝一致。
 *
 * @author Fawkes
 */
@Configuration
@RequiredArgsConstructor
public class LedgerCaptchaConfiguration {

    private final LedgerCaptchaResourceProvider ledgerCaptchaResourceProvider;
    private final LedgerCaptchaTemplateProvider ledgerCaptchaTemplateProvider;

    /**
     * 构建滑块验证码应用：深蓝看板背景 + 自绘拼图模板，有效期沿用登录验证码口径。
     *
     * @return 滑块验证码应用
     */
    @Bean(destroyMethod = "close")
    public ImageCaptchaApplication imageCaptchaApplication() {
        ResourceMap template = new ResourceMap();
        // key 必须与 Tianai 内置常量一致（含扩展名）：StandardSliderImageCaptchaGenerator 按 fixed.png / active.png 取图
        template.put("fixed.png", new Resource(LedgerCaptchaTemplateProvider.RESOURCE_TYPE, "fixed", "default"));
        template.put("active.png", new Resource(LedgerCaptchaTemplateProvider.RESOURCE_TYPE, "active", "default"));

        ImageCaptchaApplication application = TACBuilder.builder()
            .addTemplate(CaptchaTypeConstant.SLIDER, template)
            .addResource(CaptchaTypeConstant.SLIDER,
                new Resource(LedgerCaptchaResourceProvider.RESOURCE_TYPE, "machining-kanban", "default"))
            .addResource(CaptchaTypeConstant.SLIDER,
                new Resource(LedgerCaptchaResourceProvider.RESOURCE_TYPE, "machining-traveler", "default"))
            .addResource(CaptchaTypeConstant.SLIDER,
                new Resource(LedgerCaptchaResourceProvider.RESOURCE_TYPE, "machining-blueprint", "default"))
            .addResource(CaptchaTypeConstant.SLIDER,
                new Resource(LedgerCaptchaResourceProvider.RESOURCE_TYPE, "machining-cnc-tool", "default"))
            .expire("default", Constants.CAPTCHA_EXPIRATION * 60 * 1000L)
            .build();
        application.getImageCaptchaResourceManager().registerResourceProvider(ledgerCaptchaResourceProvider);
        application.getImageCaptchaResourceManager().registerResourceProvider(ledgerCaptchaTemplateProvider);
        return application;
    }
}
