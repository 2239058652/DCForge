package com.forge.dc.modules.users.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.modules.users.service.CaptchaService;
import com.forge.dc.modules.users.vo.CaptchaVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);
    private static final DefaultRedisScript<String> CAPTCHA_SCRIPT = new DefaultRedisScript<>(
            "local val = redis.call('get', KEYS[1]) " +
                    "if val then redis.call('del', KEYS[1]) end " +
                    "return val",
            String.class
    );

    private final StringRedisTemplate redisTemplate;

    public CaptchaServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CaptchaVO generate() {
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(130, 40, 4, 8);
        String uuid = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + uuid,
                captcha.getCode().toLowerCase(),
                CAPTCHA_TTL
        );
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaKey(uuid);
        vo.setBase64Img(captcha.getImageBase64Data());
        return vo;
    }

    @Override
    public void validate(String uuid, String code) {
        String key = CAPTCHA_KEY_PREFIX + uuid;

        String cached = redisTemplate.execute(CAPTCHA_SCRIPT, List.of(key));

        if (!StringUtils.hasText(cached)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码已过期");
        }

        if (!cached.equalsIgnoreCase(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }

    }
}