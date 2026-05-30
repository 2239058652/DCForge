package com.forge.dc.modules.Interface.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.modules.Interface.dto.InterfacePageDto;
import com.forge.dc.modules.Interface.entity.InterfacePermission;
import com.forge.dc.modules.Interface.service.InterfacePermissionService;
import com.forge.dc.modules.Interface.vo.InterfacePermissionVo;
import com.forge.dc.security.InterfacePermissionRuleLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/interface-permissions")
@RequiredArgsConstructor
@Tag(name = "动态权限RBAC管理")
public class InterfacePermissionController {

    private final InterfacePermissionService interfacePermissionService;
    private final InterfacePermissionRuleLoader ruleLoader;

    @GetMapping
    @PreAuthorize("hasAuthority('system:admin')")
    @Operation(summary = "查询接口权限规则列表")
    public Result<List<InterfacePermission>> list() {
        return Result.success(interfacePermissionService.listAll());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:admin')")
    @Operation(summary = "查询接口权限规则分页列表")
    public Result<PageResult<InterfacePermissionVo>> listPage(@Valid InterfacePageDto dto) {
        return Result.success(interfacePermissionService.interfacePage(dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:admin')")
    @Operation(summary = "新增/修改-接口权限规则", description = "新增接口权限规则时id为空，修改接口权限规则时id不能为空")
    public Result<Void> add(@RequestBody @Valid InterfacePermission permission) {
        interfacePermissionService.addOrEdit(permission);
        ruleLoader.refresh();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:admin')")
    @Operation(summary = "删除接口权限规则")
    public Result<Void> remove(@PathVariable Long id) {
        interfacePermissionService.removeById(id);
        ruleLoader.refresh();
        return Result.success();
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('system:admin')")
    @Operation(summary = "手动刷新接口权限缓存")
    public Result<Void> refresh() {
        ruleLoader.refresh();
        return Result.success();
    }
}