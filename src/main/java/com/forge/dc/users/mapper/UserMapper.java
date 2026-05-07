package com.forge.dc.users.mapper;

import com.forge.dc.users.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    List<SysUserEntity> getUserList();

    int registerSysUser(SysUserEntity sysUserEntity);

    boolean existsByUsername(String username);
    
}
