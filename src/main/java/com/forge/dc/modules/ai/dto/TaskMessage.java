package com.forge.dc.modules.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskMessage {
    private Long taskId;
    private Long userId;
    private String type;
    private String prompt;
    private String size;
    private List<String> images;
    private Integer retryCount;
}
