package com.forge.dc.staff.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleQueryRequest {
    @NotNull
    private Integer year;
    @NotNull
    private Integer month;
}