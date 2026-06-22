package com.forge.dc.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessageDTO {

    @NotBlank(message = "角色不能为空")
    private String role;

    @NotNull(message = "消息内容不能为空")
    private Object content;

    public boolean isValid() {
        return role != null && !role.isBlank() && content != null;
    }
}
