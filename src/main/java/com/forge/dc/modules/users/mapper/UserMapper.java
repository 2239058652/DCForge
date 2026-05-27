package com.forge.dc.modules.users.mapper;

import com.forge.dc.modules.users.entity.SysUserEntity;
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

    void bindUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    List<String> findRoleCodesByUserId(Long userId);

    List<String> findPermissionCodesByUserId(Long userId);

}
