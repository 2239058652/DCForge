package com.forge.dc.modules.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ImageHistorySaveDTO {

    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private List<String> sourceImageUrls;
}
