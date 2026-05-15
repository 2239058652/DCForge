package com.forge.dc.modules.note.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteEntity {
    private Long id;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
