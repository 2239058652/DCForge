package com.forge.dc.Interface.service;

import com.forge.dc.Interface.entity.InterfacePermission;

import java.util.List;

public interface InterfacePermissionService {
    List<InterfacePermission> listAll();

    void save(InterfacePermission permission);

    void removeById(Long id);
}