package com.forge.dc.modules.staff.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Schedule {
    private Long id;
    private Long staffId;
    private LocalDate shiftDate;
    /**
     * 0=day 1=night 2=rest
     */
    private Integer shiftType;
    private Boolean isSwapped;
    private LocalDateTime createdAt;
}