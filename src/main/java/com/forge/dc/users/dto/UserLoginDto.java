package com.forge.dc.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDto {

    @NotBlank(message = "用户名不能为空")
    @Schema(defaultValue = "admin", description = "登录用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(defaultValue = "Admin@123", description = "登录密码")
    private String password;
}
