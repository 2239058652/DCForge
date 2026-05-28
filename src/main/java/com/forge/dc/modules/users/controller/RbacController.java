package com.forge.dc.modules.users.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.modules.users.dto.*;
import com.forge.dc.modules.users.entity.SysPermissionEntity;
import com.forge.dc.modules.users.entity.SysRoleEntity;
import com.forge.dc.modules.users.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rbac")
@Tag(name = "权限管理", description = "RBAC管理的相关接口")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:list')")
    @Operation(summary = "查询角色列表")
    public Result<List<SysRoleEntity>> findRoles() {
        return Result.success(rbacService.findRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('role:add')")
    @Operation(summary = "新增角色")
    public Result<Void> addRole(@RequestBody @Valid RoleSaveDto dto) {
        rbacService.addRole(dto);
        return Result.success();
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    @Operation(summary = "更新角色")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody @Valid RoleUpdateDto dto) {
        rbacService.updateRole(id, dto);
        return Result.success();
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    @Operation(summary = "删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('permission:list')")
    @Operation(summary = "查询权限列表")
    public Result<List<SysPermissionEntity>> findPermissions() {
        return Result.success(rbacService.findPermissions());
    }

    @GetMapping("/permissions/page")
    @PreAuthorize("hasAuthority('permission:list')")
    @Operation(summary = "分页查询权限列表")
    public Result<PageResult<SysPermissionEntity>> findPermissionsByPage(@Valid PermissionPageDto dto) {
        return Result.success(rbacService.findPermissionsByPage(dto));
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('permission:add')")
    @Operation(summary = "新增权限")
    public Result<Void> addPermission(@RequestBody @Valid PermissionSaveDto dto) {
        rbacService.addPermission(dto);
        return Result.success();
    }

    @PutMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('permission:update')")
    @Operation(summary = "更新权限")
    public Result<Void> updatePermission(@PathVariable Long id, @RequestBody @Valid PermissionUpdateDto dto) {
        rbacService.updatePermission(id, dto);
        return Result.success();
    }

    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    @Operation(summary = "删除权限")
    public Result<Void> deletePermission(@PathVariable Long id) {
        rbacService.deletePermission(id);
        return Result.success();
    }

    @PutMapping("/user-roles")
    @PreAuthorize("hasAuthority('user:assign-role')")
    @Operation(summary = "分配用户角色")
    public Result<Void> assignUserRoles(@RequestBody @Valid UserRoleAssignDto dto) {
        rbacService.assignUserRoles(dto);
        return Result.success();
    }

    @PutMapping("/role-permissions")
    @PreAuthorize("hasAuthority('role:assign-permission')")
    @Operation(summary = "分配角色权限")
    public Result<Void> assignRolePermissions(@RequestBody @Valid RolePermissionAssignDto dto) {
        rbacService.assignRolePermissions(dto);
        return Result.success();
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:list')")
    @Operation(summary = "查询角色的权限列表")
    public Result<List<SysPermissionEntity>> findPermissionsByRoleId(@PathVariable Long id) {
        return Result.success(rbacService.findPermissionsByRoleId(id));
    }
}
