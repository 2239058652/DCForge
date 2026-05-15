package com.forge.dc.modules.staff.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.modules.staff.dto.StaffPageDto;
import com.forge.dc.modules.staff.dto.StaffRequest;
import com.forge.dc.modules.staff.entity.Staff;
import com.forge.dc.modules.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(name = "人员管理")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @Operation(summary = "获取所有人员")
    public Result<List<Staff>> list() {
        return Result.success(staffService.listAll());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询所有人员")
    public Result<PageResult<Staff>> page(@Valid StaffPageDto dto) {
        return Result.success(staffService.findStaffByPage(dto));
    }

    @PostMapping
    @Operation(summary = "新增人员")
    public Result<Staff> add(@RequestBody @Valid StaffRequest req) {
        return Result.success(staffService.addStaff(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改人员信息")
    public Result<Staff> update(@PathVariable Long id, @RequestBody @Valid StaffRequest req) {
        return Result.success(staffService.updateStaff(id, req));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "停用人员")
    public Result<Void> deactivate(@PathVariable Long id) {
        staffService.deactivateStaff(id);
        return Result.success();
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "启用人员")
    public Result<Void> activate(@PathVariable Long id) {
        staffService.activateStaff(id);
        return Result.success();
    }
}