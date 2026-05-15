package com.forge.dc.users.mapper;

import com.forge.dc.users.entity.SysPermissionEntity;
import com.forge.dc.users.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RbacMapper {

    List<SysRoleEntity> findRoles();

    int addRole(SysRoleEntity role);

    int updateRole(SysRoleEntity role);

    int deleteRole(Long id);

    List<SysPermissionEntity> findPermissions();

    List<SysPermissionEntity> findPermissionsByCondition(
            @Param("permissionCode") String permissionCode,
            @Param("permissionName") String permissionName,
            @Param("resourceType") String resourceType,
            @Param("status") Integer status,
            @Param("path") String path
    );

    int addPermission(SysPermissionEntity permission);

    int updatePermission(SysPermissionEntity permission);

    int deletePermission(Long id);

    int deleteUserRoles(Long userId);

    int addUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    int deleteRolePermissions(Long roleId);

    int addRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);
}
