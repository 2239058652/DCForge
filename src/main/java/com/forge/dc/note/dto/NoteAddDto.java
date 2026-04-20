package com.forge.dc.note.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoteAddDto {

    @NotBlank(message = "content不能为空")
    private String content;
}
