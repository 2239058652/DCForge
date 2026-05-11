package com.forge.dc.staff.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateScheduleRequest {
    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer year;
    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;
    /**
     * 已有排班时是否强制覆盖
     */
    private boolean forceOverwrite = false;
}