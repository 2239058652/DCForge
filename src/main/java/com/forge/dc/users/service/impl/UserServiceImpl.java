package com.forge.dc.users.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.users.dto.UserRegisterDto;
import com.forge.dc.users.entity.SysUserEntity;
import com.forge.dc.users.mapper.UserMapper;
import com.forge.dc.users.service.UserService;
import com.forge.dc.users.vo.SysUserListVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }


    @Override
    public List<SysUserListVO> findUsersAll() {
        return userMapper.getUserList()
                .stream()
                .map(this::toUserListVo)
                .toList();
    }

    @Override
    public void registerUser(UserRegisterDto userRegisterDto) {
        SysUserEntity sysUserEntity = new SysUserEntity();
        sysUserEntity.setUsername(userRegisterDto.getUsername());
        sysUserEntity.setPassword(userRegisterDto.getPassword());
        sysUserEntity.setAvatar(userRegisterDto.getAvatar());
        sysUserEntity.setNickname(userRegisterDto.getNickname());


        int rows = userMapper.registerSysUser(sysUserEntity);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "新增用户失败");
        }
    }

    private SysUserListVO toUserListVo(SysUserEntity entity) {
        SysUserListVO vo = new SysUserListVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setAvatar(entity.getAvatar());
        vo.setStatus(entity.getStatus());
        vo.setRole(entity.getRole());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
