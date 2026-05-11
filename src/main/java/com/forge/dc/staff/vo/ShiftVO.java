package com.forge.dc.staff.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShiftVO {
    private Long staffId;
    private String staffName;
    private Integer staffType;
    private Integer shiftType;
    private Boolean isSwapped;
}