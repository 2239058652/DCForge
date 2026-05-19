package com.forge.dc.modules.users.service;

import com.forge.dc.modules.users.vo.CaptchaVO;

public interface CaptchaService {
    CaptchaVO generate();

    void validate(String uuid, String code);
}