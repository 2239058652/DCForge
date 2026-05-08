package com.forge.dc.users.service;

import com.forge.dc.users.dto.PermissionSaveDto;
import com.forge.dc.users.dto.PermissionUpdateDto;
import com.forge.dc.users.dto.RolePermissionAssignDto;
import com.forge.dc.users.dto.RoleSaveDto;
import com.forge.dc.users.dto.RoleUpdateDto;
import com.forge.dc.users.dto.UserRoleAssignDto;
import com.forge.dc.users.entity.SysPermissionEntity;
import com.forge.dc.users.entity.SysRoleEntity;

import java.util.List;

public interface RbacService {

    List<SysRoleEntity> findRoles();

    void addRole(RoleSaveDto dto);

    void updateRole(Long id, RoleUpdateDto dto);

    void deleteRole(Long id);

    List<SysPermissionEntity> findPermissions();

    void addPermission(PermissionSaveDto dto);

    void updatePermission(Long id, PermissionUpdateDto dto);

    void deletePermission(Long id);

    void assignUserRoles(UserRoleAssignDto dto);

    void assignRolePermissions(RolePermissionAssignDto dto);
}
