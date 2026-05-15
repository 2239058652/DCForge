package com.forge.dc.modules.users.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUserEntity {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
