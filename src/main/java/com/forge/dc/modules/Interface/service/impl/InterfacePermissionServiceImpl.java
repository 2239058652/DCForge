package com.forge.dc.modules.Interface.service.impl;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.Interface.dto.InterfacePageDto;
import com.forge.dc.modules.Interface.entity.InterfacePermission;
import com.forge.dc.modules.Interface.mapper.InterfacePermissionMapper;
import com.forge.dc.modules.Interface.service.InterfacePermissionService;
import com.forge.dc.modules.Interface.vo.InterfacePermissionVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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
    public void addOrEdit(InterfacePermission permission) {
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

    @Override
    public PageResult<InterfacePermissionVo> interfacePage(InterfacePageDto dto) {
        // 启动分页，必须紧挨着查询方法
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());

        List<InterfacePermission> list = mapper.findInterfacePageByCondition(dto.getName(), dto.getType());
        PageInfo<InterfacePermission> pageInfo = new PageInfo<>(list);

        List<InterfacePermissionVo> interfacePermissionVoList = pageInfo.getList().stream()
                .map(InterfacePermissionVo::from).toList();

        return new PageResult<>(
                pageInfo.getTotal(),
                interfacePermissionVoList,
                pageInfo.getPageNum(),
                pageInfo.getPageSize()
        );
    }
}