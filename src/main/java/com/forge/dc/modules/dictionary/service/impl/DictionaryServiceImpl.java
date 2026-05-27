package com.forge.dc.modules.dictionary.service.impl;

import com.forge.dc.modules.dictionary.service.DictionaryService;
import com.forge.dc.modules.dictionary.vo.DictOptionVo;
import com.forge.dc.modules.users.entity.SysPermissionEntity;
import com.forge.dc.modules.users.mapper.RbacMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final RbacMapper rbacMapper;

    @Override
    public List<DictOptionVo> listPermissionCodeDictOptions() {

        List<SysPermissionEntity> list = rbacMapper.findPermissions();

        return list.stream().map(entity ->
                new DictOptionVo(entity.getPermissionName(), entity.getPermissionCode())).toList();
    }
}
