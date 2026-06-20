package com.forge.dc.modules.claude.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaudeProjectPageDto {

    @NotNull(message = "页码不能为空")
    @Min(1)
    private Integer pageNum;

    @NotNull(message = "每页数量不能为空")
    @Min(1)
    private Integer pageSize;

    private String projectPath;
}
