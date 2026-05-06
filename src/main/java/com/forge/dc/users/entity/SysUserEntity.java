package com.forge.dc.users.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUserEntity {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名，唯一
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 状态：0禁用 1启用
     */
    private Integer status;

    /**
     * 角色：USER普通用户 ADMIN管理员
     */
    private String role;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
