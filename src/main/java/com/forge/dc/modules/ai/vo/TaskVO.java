package com.forge.dc.modules.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {
    private Long id;
    private String type;
    private String status;
    private String prompt;
    private String size;
    private String objectName;
    private String revisedPrompt;
    private String errorMessage;
    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
