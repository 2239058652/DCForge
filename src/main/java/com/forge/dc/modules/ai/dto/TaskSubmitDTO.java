package com.forge.dc.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class TaskSubmitDTO {

    @NotBlank(message = "type 不能为空")
    private String type;

    @NotBlank(message = "prompt 不能为空")
    private String prompt;

    private String size;

    private List<String> images;
}
