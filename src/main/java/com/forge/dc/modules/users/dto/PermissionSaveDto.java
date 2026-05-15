package com.forge.dc.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionSaveDto {

    @NotBlank(message = "permissionCode cannot be blank")
    private String permissionCode;

    @NotBlank(message = "permissionName cannot be blank")
    private String permissionName;

    private String resourceType = "API";
    private String path;
    private Integer status = 1;
}
