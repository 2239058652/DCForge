package com.forge.dc.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ImageToImageDTO {

    @NotBlank(message = "prompt 不能为空")
    private String prompt;

    @NotBlank(message = "size 不能为空")
    private String size = "1024x768";

    @NotEmpty(message = "image 不能为空")
    private List<String> images;
}
