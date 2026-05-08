package com.forge.dc.users.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.users.dto.PermissionSaveDto;
import com.forge.dc.users.dto.PermissionUpdateDto;
import com.forge.dc.users.dto.RolePermissionAssignDto;
import com.forge.dc.users.dto.RoleSaveDto;
import com.forge.dc.users.dto.RoleUpdateDto;
import com.forge.dc.users.dto.UserRoleAssignDto;
import com.forge.dc.users.entity.SysPermissionEntity;
import com.forge.dc.users.entity.SysRoleEntity;
import com.forge.dc.users.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rbac")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<List<SysRoleEntity>> findRoles() {
        return Result.success(rbacService.findRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('role:add')")
    public Result<Void> addRole(@RequestBody @Valid RoleSaveDto dto) {
        rbacService.addRole(dto);
        return Result.success();
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody @Valid RoleUpdateDto dto) {
        rbacService.updateRole(id, dto);
        return Result.success();
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('permission:list')")
    public Result<List<SysPermissionEntity>> findPermissions() {
        return Result.success(rbacService.findPermissions());
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('permission:add')")
    public Result<Void> addPermission(@RequestBody @Valid PermissionSaveDto dto) {
        rbacService.addPermission(dto);
        return Result.success();
    }

    @PutMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('permission:update')")
    public Result<Void> updatePermission(@PathVariable Long id, @RequestBody @Valid PermissionUpdateDto dto) {
        rbacService.updatePermission(id, dto);
        return Result.success();
    }

    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public Result<Void> deletePermission(@PathVariable Long id) {
        rbacService.deletePermission(id);
        return Result.success();
    }

    @PutMapping("/user-roles")
    @PreAuthorize("hasAuthority('user:assign-role')")
    public Result<Void> assignUserRoles(@RequestBody @Valid UserRoleAssignDto dto) {
        rbacService.assignUserRoles(dto);
        return Result.success();
    }

    @PutMapping("/role-permissions")
    @PreAuthorize("hasAuthority('role:assign-permission')")
    public Result<Void> assignRolePermissions(@RequestBody @Valid RolePermissionAssignDto dto) {
        rbacService.assignRolePermissions(dto);
        return Result.success();
    }
}
