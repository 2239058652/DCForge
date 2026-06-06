package com.forge.dc.modules.dictionary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DictPageDto {

    @Min(value = 1, message = "页码最小为1")
    private int pageNum = 1;

    @Min(1)
    @Max(value = 100, message = "每页最多100条")
    private int pageSize = 10;

    private String dictCode;
}
