package com.forge.dc.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionRequestDTO {

    @NotEmpty(message = "消息列表不能为空")
    @Valid
    private List<ChatMessageDTO> messages;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Boolean stream;

    private String systemPrompt;

    private JsonNode tools;

    @JsonProperty("tool_choice")
    private JsonNode toolChoice;

    private JsonNode stop;

    @JsonProperty("chat_template_kwargs")
    private JsonNode chatTemplateKwargs;
}
