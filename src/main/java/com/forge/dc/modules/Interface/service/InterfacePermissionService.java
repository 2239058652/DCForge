package com.forge.dc.modules.Interface.service;

import com.forge.dc.modules.Interface.entity.InterfacePermission;

import java.util.List;

public interface InterfacePermissionService {
    List<InterfacePermission> listAll();

    void save(InterfacePermission permission);

    void removeById(Long id);
}