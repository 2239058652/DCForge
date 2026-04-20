package com.forge.dc.note.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteEntity {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
