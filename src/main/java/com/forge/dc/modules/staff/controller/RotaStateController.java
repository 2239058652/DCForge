package com.forge.dc.modules.staff.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.modules.staff.entity.RotaState;
import com.forge.dc.modules.staff.entity.Staff;
import com.forge.dc.modules.staff.mapper.RotaStateMapper;
import com.forge.dc.modules.staff.mapper.StaffMapper;
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

    @Operation(summary = "查看当前夜班队列状态")
    @GetMapping
    public Result<Map<String, Object>> getState() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<Integer, String> typeNames = Map.of(0, "doctor", 1, "nurse", 2, "receptionist");
        for (Map.Entry<Integer, String> entry : typeNames.entrySet()) {
            int type = entry.getKey();
            RotaState state = rotaStateMapper.findByType(type);
            List<Staff> queue = staffMapper.findActiveByType(type);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("queue", queue);
            info.put("nextStaffId", state != null ? state.getCurrentStaffId() : null);
            result.put(entry.getValue(), info);
        }
        return Result.success(result);
    }
}