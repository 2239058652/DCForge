package com.forge.dc.note.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteListVo {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
