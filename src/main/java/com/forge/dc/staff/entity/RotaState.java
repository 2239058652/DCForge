package com.forge.dc.staff.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RotaState {
    private Long id;
    private Integer type;
    private Long currentStaffId;
    private LocalDateTime updatedAt;
}