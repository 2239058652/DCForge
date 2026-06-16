package com.forge.dc.modules.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiImageHistoryEntity {

    private Long id;
    private Long userId;
    private String type;
    private String prompt;
    private String revisedPrompt;
    private String sourceImageUrl;
    private String objectName;
    private String size;
    private LocalDateTime createdAt;
}
