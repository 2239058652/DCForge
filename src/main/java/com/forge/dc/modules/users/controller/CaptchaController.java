package com.forge.dc.modules.users.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.modules.users.service.CaptchaService;
import com.forge.dc.modules.users.vo.CaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/captcha")
@Tag(name = "验证码", description = "验证码相关接口")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/generate")
    @Operation(summary = "生成验证码")
    public Result<CaptchaVO> generate() {
        return Result.success(captchaService.generate());
    }
}