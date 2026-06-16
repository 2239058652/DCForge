package com.forge.dc.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TextToImageDTO {

    @NotBlank(message = "prompt 不能为空")
    private String prompt;

    @NotBlank(message = "size 不能为空")
    private String size = "1024x768";
}
