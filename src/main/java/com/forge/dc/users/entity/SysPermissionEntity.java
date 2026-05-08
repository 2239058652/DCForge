package com.forge.dc.users.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermissionEntity {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private String path;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
