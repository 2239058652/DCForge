package com.forge.dc.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionUpdateDto {

    @NotBlank(message = "permissionCode cannot be blank")
    private String permissionCode;

    @NotBlank(message = "permissionName cannot be blank")
    private String permissionName;

    private String resourceType;
    private String path;
    private Integer status;
}
