package com.forge.dc.modules.users.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUserRoleEntity {

    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime createdAt;
}
