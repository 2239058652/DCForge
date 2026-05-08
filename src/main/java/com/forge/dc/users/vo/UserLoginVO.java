package com.forge.dc.users.vo;

import lombok.Data;

@Data
public class UserLoginVO {

    /**
     * 登录令牌
     */
    private String token;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 角色
     */
    private String role;
}