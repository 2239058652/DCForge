package com.forge.dc.modules.users.vo;

import lombok.Data;

@Data
public class CaptchaVO {
    private String captchaKey;
    private String base64Img;
}