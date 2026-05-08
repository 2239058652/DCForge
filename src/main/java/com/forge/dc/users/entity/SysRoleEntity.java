package com.forge.dc.users.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRoleEntity {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
