package com.forge.dc.users.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.common.util.JwtUtils;
import com.forge.dc.users.dto.UserLoginDto;
import com.forge.dc.users.dto.UserRegisterDto;
import com.forge.dc.users.entity.SysUserEntity;
import com.forge.dc.users.mapper.UserMapper;
import com.forge.dc.users.service.UserService;
import com.forge.dc.users.vo.SysUserListVO;
import com.forge.dc.users.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
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

        // 判断是否已经存在该用户？
        checkUsernameUnique(userRegisterDto.getUsername());

        // 密码加密
        String encodedPassword = passwordEncoder.encode(userRegisterDto.getPassword());

        SysUserEntity sysUserEntity = new SysUserEntity();
        sysUserEntity.setUsername(userRegisterDto.getUsername());
        sysUserEntity.setPassword(encodedPassword);
        sysUserEntity.setAvatar(userRegisterDto.getAvatar());
        sysUserEntity.setNickname(userRegisterDto.getNickname());


        try {
            int rows = userMapper.registerSysUser(sysUserEntity);
            if (rows <= 0) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "新增用户失败");
            }
        } catch (DuplicateKeyException e) {
            log.warn("注册用户失败，用户名已存在：{}", userRegisterDto.getUsername(), e);
            throw new BusinessException(ResultCode.ALREADY_EXISTS.getCode(), "用户名已存在");
        }
    }

    @Override
    public UserLoginVO login(UserLoginDto userLoginDto) {
        SysUserEntity user = userMapper.findByUsername(userLoginDto.getUsername());

        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        if (!passwordEncoder.matches(userLoginDto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已被禁用");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserLoginVO vo = new UserLoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setToken(token);

        return vo;
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

    private void checkUsernameUnique(String username) {
        if (userMapper.existsByUsername(username)) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS.getCode(), "用户名已存在");
        }
    }
}
