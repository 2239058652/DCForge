package com.forge.dc.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ImageHistorySaveDTO {

    @NotBlank(message = "生成类型不能为空")
    private String type;

    @NotBlank(message = "prompt 不能为空")
    private String prompt;

    private String revisedPrompt;

    @NotBlank(message = "图片 URL 不能为空")
    private String imageUrl;

    private List<String> sourceImageUrls;

    private String size;
}
