package com.forge.dc.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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

    @JsonProperty("tool_call_id")
    private String toolCallId;

    @JsonProperty("tool_calls")
    private JsonNode toolCalls;

    public boolean isValid() {
        if (role == null || role.isBlank()) {
            return false;
        }
        if ("assistant".equals(role) && toolCalls != null && !toolCalls.isNull()) {
            return true;
        }
        if ("tool".equals(role)) {
            return toolCallId != null && !toolCallId.isBlank() && content != null;
        }
        return content != null;
    }
}
