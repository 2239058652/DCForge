package com.forge.dc.modules.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class StreamChatRequestDTO {

    private String sessionId;

    private List<ChatMessageDTO> messages;

    private Double temperature;

    private Integer maxTokens;

    private String systemPrompt;
}
