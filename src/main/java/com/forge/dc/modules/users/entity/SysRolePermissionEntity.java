package com.forge.dc.modules.users.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRolePermissionEntity {

    private Long id;
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createdAt;
}
