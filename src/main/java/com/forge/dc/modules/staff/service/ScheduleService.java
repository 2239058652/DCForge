package com.forge.dc.modules.staff.service;

import com.forge.dc.modules.staff.entity.Schedule;
import com.forge.dc.modules.staff.vo.DailyScheduleVO;

import java.util.List;

public interface ScheduleService {
    void generateSchedule(int year, int month, boolean forceOverwrite);

    List<DailyScheduleVO> getMonthlySchedule(int year, int month);

    List<Schedule> getStaffSchedule(Long staffId, int year, int month);

    void deleteSchedule(int year, int month);
}