package com.forge.dc.staff.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleDetailDO {
    private Long id;
    private Long staffId;
    private String staffName;
    private Integer staffType;
    private LocalDate shiftDate;
    private Integer shiftType;
    private Boolean isSwapped;
}