package com.forge.dc.modules.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiTaskEntity {
    private Long id;
    private Long userId;
    private String type;
    private String status;
    private String prompt;
    private String size;
    private String images;
    private String objectName;
    private String revisedPrompt;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
