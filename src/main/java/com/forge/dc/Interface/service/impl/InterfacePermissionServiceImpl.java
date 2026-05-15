package com.forge.dc.Interface.service.impl;

import com.forge.dc.Interface.entity.InterfacePermission;
import com.forge.dc.Interface.mapper.InterfacePermissionMapper;
import com.forge.dc.Interface.service.InterfacePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterfacePermissionServiceImpl implements InterfacePermissionService {

    private final InterfacePermissionMapper mapper;

    @Override
    public List<InterfacePermission> listAll() {
        return mapper.listAll();
    }

    @Override
    public void save(InterfacePermission permission) {
        if (permission.getId() == null) {
            mapper.insert(permission);
        } else {
            mapper.update(permission);
        }
    }

    @Override
    public void removeById(Long id) {
        mapper.deleteById(id);
    }
}