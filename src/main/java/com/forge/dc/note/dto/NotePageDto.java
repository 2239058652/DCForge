package com.forge.dc.note.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotePageDto {
    @NotNull(message = "pageNum不能为空")
    @Min(value = 1, message = "pageNum最小为1")
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    @Min(value = 1, message = "pageSize最小为1")
    private Integer pageSize;

    private String content;
    private Integer offset;
}
