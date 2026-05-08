package com.forge.dc.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleSaveDto {

    @NotBlank(message = "roleCode cannot be blank")
    private String roleCode;

    @NotBlank(message = "roleName cannot be blank")
    private String roleName;

    private Integer status = 1;
}
