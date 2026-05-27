package com.forge.dc.modules.users.service;

import com.forge.dc.modules.users.dto.UserLoginDto;
import com.forge.dc.modules.users.dto.UserRegisterDto;
import com.forge.dc.modules.users.vo.SysUserListVO;
import com.forge.dc.modules.users.vo.UserLoginVO;
import jakarta.validation.Valid;

import java.util.List;

public interface UserService {

    List<SysUserListVO> findUsersAll();

    UserLoginVO registerUser(UserRegisterDto userRegisterDto);

    UserLoginVO login(@Valid UserLoginDto userLoginDto);

    void logout(String token);
}
