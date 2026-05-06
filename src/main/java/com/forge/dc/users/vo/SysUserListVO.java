package com.forge.dc.users.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUserListVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Integer status;

    private String role;

    private LocalDateTime createdAt;
}