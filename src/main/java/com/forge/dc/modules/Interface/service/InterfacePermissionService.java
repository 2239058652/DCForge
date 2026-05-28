package com.forge.dc.modules.Interface.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.Interface.dto.InterfacePageDto;
import com.forge.dc.modules.Interface.entity.InterfacePermission;
import com.forge.dc.modules.Interface.vo.InterfacePermissionVo;
import jakarta.validation.Valid;

import java.util.List;

public interface InterfacePermissionService {
    List<InterfacePermission> listAll();

    void save(InterfacePermission permission);

    void removeById(Long id);

    PageResult<InterfacePermissionVo> interfacePage(@Valid InterfacePageDto dto);
}