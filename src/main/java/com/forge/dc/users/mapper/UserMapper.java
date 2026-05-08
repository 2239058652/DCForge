package com.forge.dc.users.mapper;

import com.forge.dc.users.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    List<SysUserEntity> getUserList();

    int registerSysUser(SysUserEntity sysUserEntity);

    boolean existsByUsername(String username);

    SysUserEntity findByUsername(String username);

    Long findRoleIdByCode(String roleCode);

    int bindUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    List<String> findRoleCodesByUserId(Long userId);

    List<String> findPermissionCodesByUserId(Long userId);

}
