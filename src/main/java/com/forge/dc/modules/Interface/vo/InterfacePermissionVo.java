package com.forge.dc.modules.Interface.vo;

import com.forge.dc.modules.Interface.entity.InterfacePermission;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterfacePermissionVo {
    private Long id;
    private String httpMethod;
    private String urlPattern;
    private String permissionCode;
    private String type;
    private String description;
    private LocalDateTime createdAt;

    public static InterfacePermissionVo from(InterfacePermission interfacePermission) {
        InterfacePermissionVo vo = new InterfacePermissionVo();
        vo.setId(interfacePermission.getId());
        vo.setHttpMethod(interfacePermission.getHttpMethod());
        vo.setUrlPattern(interfacePermission.getUrlPattern());
        vo.setPermissionCode(interfacePermission.getPermissionCode());
        vo.setType(interfacePermission.getType());
        vo.setDescription(interfacePermission.getDescription());
        vo.setCreatedAt(interfacePermission.getCreatedAt());
        return vo;
    }
}
