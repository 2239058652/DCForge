package com.forge.dc.modules.localport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class TerminatePortProcessDto {

    @NotNull(message = "PID不能为空")
    private Long pid;

    @NotBlank(message = "协议不能为空")
    @Pattern(regexp = "^(tcp|udp)$", message = "协议只允许 tcp 或 udp")
    private String protocol;

    @NotNull(message = "端口不能为空")
    @Max(value = 65535, message = "端口范围为 1-65535")
    private Integer port;

    @NotNull(message = "确认标识不能为空")
    private Boolean confirm;
}
