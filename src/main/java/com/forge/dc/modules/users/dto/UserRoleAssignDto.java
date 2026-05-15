package com.forge.dc.modules.users.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleAssignDto {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @NotEmpty(message = "roleIds cannot be empty")
    private List<Long> roleIds;
}
