package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.IdUtil;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.web.config.properties.CaptchaProperties;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 滑块行为验证码：按 tianai-captcha 官方前端 SDK 协议提供「生成 / 校验」两个接口。
 * 校验通过后把一次性票据写入 Redis，登录仍沿用原有 code + uuid 字段，
 * 因此 PasswordAuthStrategy / SysRegisterService / LoginBody 无需改动。
 * 响应使用框架统一的 R：其成功码 200 与 SDK 约定一致，SDK 直接按 code===200 判成功。
 *
 * @author Fawkes
 */
@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/code")
public class SliderCaptchaController {

    /**
     * 校验失败码：SDK 对 4001 以外的非 200 码会显示「验证码被黑洞吸走了」，
     * 用 4001 才能让 SDK 展示自身的失败文案并重载拼图。
     */
    private static final int SDK_CHECK_FAIL_CODE = 4001;

    /** 已消费的拼图 id：防止同一张拼图被反复换取登录票据 */
    private static final String SLIDER_USED_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "slider_used:";

    private final ImageCaptchaApplication imageCaptchaApplication;
    private final CaptchaProperties captchaProperties;

    /**
     * 生成滑块拼图：返回 SDK 所需 {id, type, backgroundImage, templateImage, ...}。
     *
     * @return 符合前端 tac SDK 生成协议的响应体
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/slider")
    public R<Map<String, Object>> generate() {
        if (!Boolean.TRUE.equals(captchaProperties.getEnable())) {
            // 验证码关闭时 SDK 走 DISABLED 分支，前端不显示拼图
            Map<String, Object> disabled = new HashMap<>(2);
            disabled.put("type", "DISABLED");
            return R.ok(disabled);
        }
        ApiResponse<ImageCaptchaVO> response = imageCaptchaApplication.generateCaptcha(CaptchaTypeConstant.SLIDER);
        if (!response.isSuccess() || response.getData() == null) {
            log.warn("滑块验证码生成失败: {}", response.getMsg());
            return R.fail(response.getMsg() == null ? "验证码生成失败，请重试" : response.getMsg());
        }
        ImageCaptchaVO captcha = response.getData();
        Map<String, Object> data = new HashMap<>(8);
        data.put("id", captcha.getId());
        data.put("type", captcha.getType());
        data.put("backgroundImage", captcha.getBackgroundImage());
        data.put("templateImage", captcha.getTemplateImage());
        data.put("backgroundImageTag", captcha.getBackgroundImageTag());
        data.put("templateImageTag", captcha.getTemplateImageTag());
        data.put("backgroundImageWidth", captcha.getBackgroundImageWidth());
        data.put("backgroundImageHeight", captcha.getBackgroundImageHeight());
        data.put("templateImageWidth", captcha.getTemplateImageWidth());
        data.put("templateImageHeight", captcha.getTemplateImageHeight());
        return R.ok(data);
    }

    /**
     * 服务端校验滑动轨迹：matching() 用 id 取出生成时保存的缺口坐标并比对轨迹。
     * 通过后签发 uuid + code 票据，前端把它们回填到登录/注册表单。
     *
     * @param body SDK 回传的 {id, data: 轨迹}
     * @return 成功时 data 为 {id, uuid, code}，失败时 code 为 4001
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/check")
    public R<Map<String, Object>> check(@RequestBody SliderCheckBody body) {
        if (body == null || body.getId() == null || body.getData() == null) {
            return R.fail(SDK_CHECK_FAIL_CODE, "验证参数不完整");
        }
        // 官方前端 SDK 上报的是页面绝对坐标，而服务端校验器要求以起点为原点的相对位移，
        // 此处按轨迹首点归一化，否则首点会被判定为越界而直接判失败。
        normalizeTrack(body.getData());
        ApiResponse<?> match = imageCaptchaApplication.matching(body.getId(), body.getData());
        if (!match.isSuccess()) {
            log.info("滑块验证未通过: id={}, msg={}", body.getId(), match.getMsg());
            return R.fail(SDK_CHECK_FAIL_CODE, match.getMsg() == null ? "验证失败，请重试" : match.getMsg());
        }
        // matching 会消费该 id 的缓存，但这里再落一道「已使用」标记，防止并发/重放重复换票据
        String usedKey = SLIDER_USED_KEY + body.getId();
        if (RedisUtils.getCacheObject(usedKey) != null) {
            return R.fail(SDK_CHECK_FAIL_CODE, "验证已失效，请重新验证");
        }
        RedisUtils.setCacheObject(usedKey, Boolean.TRUE, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        String uuid = IdUtil.simpleUUID();
        String code = IdUtil.simpleUUID();
        RedisUtils.setCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + uuid, code,
            Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        Map<String, Object> data = new HashMap<>(3);
        data.put("id", body.getId());
        data.put("uuid", uuid);
        data.put("code", code);
        return R.ok(data);
    }

    /**
     * 将轨迹点换算为相对首点的位移坐标。
     * 官方 SDK 上报的是视口绝对坐标，服务端校验要求首点落在原点附近（|x|、|y| ≤ 10），
     * 因此以首点为基准做一次平移；平移不改变位移量与时序，不影响服务端的行为特征判定。
     *
     * @param track 滑动轨迹
     */
    private static void normalizeTrack(ImageCaptchaTrack track) {
        List<ImageCaptchaTrack.Track> trackList = track.getTrackList();
        if (trackList == null || trackList.isEmpty()) {
            return;
        }
        ImageCaptchaTrack.Track first = trackList.get(0);
        float originX = first.getX() == null ? 0f : first.getX();
        float originY = first.getY() == null ? 0f : first.getY();
        for (ImageCaptchaTrack.Track point : trackList) {
            if (point.getX() != null) {
                point.setX(point.getX() - originX);
            }
            if (point.getY() != null) {
                point.setY(point.getY() - originY);
            }
        }
    }

    /**
     * SDK 校验请求体：id 为拼图标识，data 为滑动轨迹（含 trackList / left / top 等）。
     */
    @Data
    public static class SliderCheckBody {
        private String id;
        private ImageCaptchaTrack data;
    }
}
