package com.forge.dc.users.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.users.dto.PermissionSaveDto;
import com.forge.dc.users.dto.PermissionUpdateDto;
import com.forge.dc.users.dto.RolePermissionAssignDto;
import com.forge.dc.users.dto.RoleSaveDto;
import com.forge.dc.users.dto.RoleUpdateDto;
import com.forge.dc.users.dto.UserRoleAssignDto;
import com.forge.dc.users.entity.SysPermissionEntity;
import com.forge.dc.users.entity.SysRoleEntity;
import com.forge.dc.users.mapper.RbacMapper;
import com.forge.dc.users.service.RbacService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RbacServiceImpl implements RbacService {

    private final RbacMapper rbacMapper;

    public RbacServiceImpl(RbacMapper rbacMapper) {
        this.rbacMapper = rbacMapper;
    }

    @Override
    public List<SysRoleEntity> findRoles() {
        return rbacMapper.findRoles();
    }

    @Override
    public void addRole(RoleSaveDto dto) {
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setStatus(dto.getStatus());
        checkRows(rbacMapper.addRole(role), "add role failed");
    }

    @Override
    public void updateRole(Long id, RoleUpdateDto dto) {
        SysRoleEntity role = new SysRoleEntity();
        role.setId(id);
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setStatus(dto.getStatus());
        checkRows(rbacMapper.updateRole(role), "update role failed");
    }

    @Override
    public void deleteRole(Long id) {
        checkRows(rbacMapper.deleteRole(id), "delete role failed");
    }

    @Override
    public List<SysPermissionEntity> findPermissions() {
        return rbacMapper.findPermissions();
    }

    @Override
    public void addPermission(PermissionSaveDto dto) {
        SysPermissionEntity permission = new SysPermissionEntity();
        permission.setPermissionCode(dto.getPermissionCode());
        permission.setPermissionName(dto.getPermissionName());
        permission.setResourceType(dto.getResourceType());
        permission.setPath(dto.getPath());
        permission.setStatus(dto.getStatus());
        checkRows(rbacMapper.addPermission(permission), "add permission failed");
    }

    @Override
    public void updatePermission(Long id, PermissionUpdateDto dto) {
        SysPermissionEntity permission = new SysPermissionEntity();
        permission.setId(id);
        permission.setPermissionCode(dto.getPermissionCode());
        permission.setPermissionName(dto.getPermissionName());
        permission.setResourceType(dto.getResourceType());
        permission.setPath(dto.getPath());
        permission.setStatus(dto.getStatus());
        checkRows(rbacMapper.updatePermission(permission), "update permission failed");
    }

    @Override
    public void deletePermission(Long id) {
        checkRows(rbacMapper.deletePermission(id), "delete permission failed");
    }

    @Override
    public void assignUserRoles(UserRoleAssignDto dto) {
        rbacMapper.deleteUserRoles(dto.getUserId());
        checkRows(rbacMapper.addUserRoles(dto.getUserId(), dto.getRoleIds()), "assign user roles failed");
    }

    @Override
    public void assignRolePermissions(RolePermissionAssignDto dto) {
        rbacMapper.deleteRolePermissions(dto.getRoleId());
        checkRows(rbacMapper.addRolePermissions(dto.getRoleId(), dto.getPermissionIds()), "assign role permissions failed");
    }

    private void checkRows(int rows, String message) {
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), message);
        }
    }
}
