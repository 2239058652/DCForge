package com.forge.dc.users.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.users.dto.*;
import com.forge.dc.users.entity.SysPermissionEntity;
import com.forge.dc.users.entity.SysRoleEntity;

import java.util.List;

public interface RbacService {

    List<SysRoleEntity> findRoles();

    void addRole(RoleSaveDto dto);

    void updateRole(Long id, RoleUpdateDto dto);

    void deleteRole(Long id);

    List<SysPermissionEntity> findPermissions();

    PageResult<SysPermissionEntity> findPermissionsByPage(PermissionPageDto dto);

    void addPermission(PermissionSaveDto dto);

    void updatePermission(Long id, PermissionUpdateDto dto);

    void deletePermission(Long id);

    void assignUserRoles(UserRoleAssignDto dto);

    void assignRolePermissions(RolePermissionAssignDto dto);
}
