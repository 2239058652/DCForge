package com.forge.dc.staff.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyScheduleVO {
    private LocalDate date;
    private List<ShiftVO> shifts;
}