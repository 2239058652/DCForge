package com.forge.dc.users.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.common.util.JwtUtils;
import com.forge.dc.common.util.UserAuthCacheManagerUtils;
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

    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserAuthCacheManagerUtils cacheManager;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils, UserAuthCacheManagerUtils cacheManager) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.cacheManager = cacheManager;
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
        checkUsernameUnique(userRegisterDto.getUsername());

        String encodedPassword = passwordEncoder.encode(userRegisterDto.getPassword());

        SysUserEntity sysUserEntity = new SysUserEntity();
        sysUserEntity.setUsername(userRegisterDto.getUsername());
        sysUserEntity.setPassword(encodedPassword);
        sysUserEntity.setAvatar(userRegisterDto.getAvatar());
        sysUserEntity.setNickname(userRegisterDto.getNickname());

        try {
            int rows = userMapper.registerSysUser(sysUserEntity);
            if (rows <= 0) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "add user failed");
            }
            bindDefaultRole(sysUserEntity.getId());
        } catch (DuplicateKeyException e) {
            log.warn("register user failed, username exists: {}", userRegisterDto.getUsername(), e);
            throw new BusinessException(ResultCode.ALREADY_EXISTS.getCode(), "username already exists");
        }
    }

    @Override
    public UserLoginVO login(UserLoginDto userLoginDto) {
        SysUserEntity user = userMapper.findByUsername(userLoginDto.getUsername());

        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "username or password error");
        }

        if (!passwordEncoder.matches(userLoginDto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "username or password error");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "user disabled");
        }

        List<String> roles = userMapper.findRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());

        // 写入 Redis 缓存
        cacheManager.save(user.getId(), roles, permissions);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        UserLoginVO vo = new UserLoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roles);
        vo.setPermissions(permissions);
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
        vo.setRoles(userMapper.findRoleCodesByUserId(entity.getId()));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private void checkUsernameUnique(String username) {
        if (userMapper.existsByUsername(username)) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS.getCode(), "username already exists");
        }
    }

    private void bindDefaultRole(Long userId) {
        Long roleId = userMapper.findRoleIdByCode(DEFAULT_ROLE_CODE);
        if (roleId == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "default role not found");
        }
        userMapper.bindUserRole(userId, roleId);
    }
}
