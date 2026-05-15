package com.forge.dc.modules.Interface.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterfacePermission {
    private Long id;
    private String httpMethod;
    private String urlPattern;
    private String permissionCode;
    private String description;
    private LocalDateTime createdAt;
}