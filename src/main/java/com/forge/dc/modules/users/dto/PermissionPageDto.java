package com.forge.dc.modules.users.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PermissionPageDto {

    @Min(value = 1, message = "页码最小为1")
    private int pageNum = 1;

    @Min(1)
    @Max(value = 100, message = "每页最多100条")
    private int pageSize = 10;

    // 以下为可选的筛选条件
    private String permissionCode;   // 权限编码，支持模糊搜索
    private String permissionName;   // 权限名称，支持模糊搜索
    private String resourceType;     // 资源类型，精确匹配
    private Integer status;          // 状态，精确匹配
    private String path;             // 路径，支持模糊搜索
}