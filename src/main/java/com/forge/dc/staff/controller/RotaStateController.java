package com.forge.dc.staff.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.staff.entity.RotaState;
import com.forge.dc.staff.entity.Staff;
import com.forge.dc.staff.mapper.RotaStateMapper;
import com.forge.dc.staff.mapper.StaffMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rota-state")
@RequiredArgsConstructor
@Tag(name = "夜班队列状态")
public class RotaStateController {

    private final RotaStateMapper rotaStateMapper;
    private final StaffMapper staffMapper;

    @GetMapping
    @Operation(summary = "查看当前夜班队列状态")
    public Result<Map<String, Object>> getState() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int type : new int[]{0, 1}) {
            RotaState state = rotaStateMapper.findByType(type);
            List<Staff> queue = staffMapper.findActiveByType(type);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("queue", queue);
            info.put("nextStaffId", state != null ? state.getCurrentStaffId() : null);
            result.put(type == 0 ? "doctor" : "nurse", info);
        }
        return Result.success(result);
    }
}