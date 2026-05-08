package com.forge.dc.users.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.users.dto.UserLoginDto;
import com.forge.dc.users.dto.UserRegisterDto;
import com.forge.dc.users.service.UserService;
import com.forge.dc.users.vo.SysUserListVO;
import com.forge.dc.users.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "用户管理", description = "用户管理的增删改查相关接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 注册用户
     *
     */
    @Operation(summary = "注册用户")
    @PostMapping("/register")
    public Result<Void> registerUser(@RequestBody @Valid UserRegisterDto userRegisterDto) {
        userService.registerUser(userRegisterDto);
        return Result.success();
    }

    /**
     * 查询用户列表（全部）
     *
     */
    @Operation(summary = "查询用户列表（全部）")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<List<SysUserListVO>> findAllUsersList() {
        return Result.success(userService.findUsersAll());
    }

    /**
     * 用户登录
     *
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDto userLoginDto) {
        return Result.success("登录成功", userService.login(userLoginDto));
    }

}
