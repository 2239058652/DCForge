package com.forge.dc.users.service;

import com.forge.dc.users.dto.UserLoginDto;
import com.forge.dc.users.dto.UserRegisterDto;
import com.forge.dc.users.vo.SysUserListVO;
import com.forge.dc.users.vo.UserLoginVO;
import jakarta.validation.Valid;

import java.util.List;

public interface UserService {

    List<SysUserListVO> findUsersAll();

    void registerUser(UserRegisterDto userRegisterDto);

    UserLoginVO login(@Valid UserLoginDto userLoginDto);
}
