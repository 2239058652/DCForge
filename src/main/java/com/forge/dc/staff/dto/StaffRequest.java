package com.forge.dc.staff.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffRequest {
    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer type;

    @NotNull
    @Min(0)
    @Max(6)
    private Integer restDay;
    
    /**
     * 不传则加到末尾
     */
    private Integer nightOrder;
}