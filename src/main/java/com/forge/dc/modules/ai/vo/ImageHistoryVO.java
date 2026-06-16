package com.forge.dc.modules.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImageHistoryVO {

    private Long id;
    private String type;
    private String prompt;
    private String revisedPrompt;
    private String sourceImageUrl;
    private String imageUrl;
    private String size;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
