package com.forge.dc.users.service;

import com.forge.dc.users.dto.UserRegisterDto;
import com.forge.dc.users.vo.SysUserListVO;

import java.util.List;

public interface UserService {

    List<SysUserListVO> findUsersAll();

    void registerUser(UserRegisterDto userRegisterDto);
}
