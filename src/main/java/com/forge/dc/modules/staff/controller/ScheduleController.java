package com.forge.dc.modules.staff.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.modules.staff.dto.GenerateScheduleRequest;
import com.forge.dc.modules.staff.entity.Schedule;
import com.forge.dc.modules.staff.service.ScheduleService;
import com.forge.dc.modules.staff.vo.DailyScheduleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Tag(name = "排班管理")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/generate")
    @Operation(summary = "生成指定年月排班")
    public Result<Void> generate(@RequestBody @Valid GenerateScheduleRequest req) {
        scheduleService.generateSchedule(req.getYear(), req.getMonth(), req.isForceOverwrite());
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "获取某月排班（月视图）")
    public Result<List<DailyScheduleVO>> monthly(@RequestParam int year, @RequestParam int month) {
        return Result.success(scheduleService.getMonthlySchedule(year, month));
    }

    @GetMapping("/staff/{staffId}")
    @Operation(summary = "获取某人某月排班")
    public Result<List<Schedule>> staffSchedule(@PathVariable Long staffId,
                                                @RequestParam int year,
                                                @RequestParam int month) {
        return Result.success(scheduleService.getStaffSchedule(staffId, year, month));
    }

    @DeleteMapping
    @Operation(summary = "删除某月排班")
    public Result<Void> delete(@RequestParam int year, @RequestParam int month) {
        scheduleService.deleteSchedule(year, month);
        return Result.success();
    }
}