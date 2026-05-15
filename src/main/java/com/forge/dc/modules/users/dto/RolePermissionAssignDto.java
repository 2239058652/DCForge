package com.forge.dc.modules.users.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionAssignDto {

    @NotNull(message = "roleId cannot be null")
    private Long roleId;

    @NotEmpty(message = "permissionIds cannot be empty")
    private List<Long> permissionIds;
}
