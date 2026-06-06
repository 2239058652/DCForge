package com.forge.dc.modules.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictSaveDto {

    private Long id;

    @NotBlank(message = "字典编码不能为空")
    private String dictCode;

    @NotBlank(message = "显示名不能为空")
    private String dictLabel;

    @NotBlank(message = "存储值不能为空")
    private String dictValue;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
