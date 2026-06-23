package com.forge.dc.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * Anthropic Messages API 请求 DTO
 * @see https://docs.anthropic.com/claude/reference/getting-started-with-the-api
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnthropicRequestDTO {

    private String model;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    // system 可以是字符串或数组格式
    private JsonNode system;

    private List<AnthropicMessage> messages;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    private Boolean stream;

    private JsonNode tools;

    @JsonProperty("tool_choice")
    private JsonNode toolChoice;

    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    private JsonNode thinking;

    private JsonNode metadata;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicMessage {
        private String role;
        // 使用 JsonNode 兼容字符串和数组两种格式
        private JsonNode content;
    }
}
