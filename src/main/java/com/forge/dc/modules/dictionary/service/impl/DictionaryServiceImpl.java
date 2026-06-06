package com.forge.dc.modules.dictionary.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.modules.dictionary.dto.DictPageDto;
import com.forge.dc.modules.dictionary.dto.DictSaveDto;
import com.forge.dc.modules.dictionary.entity.SysDictEntity;
import com.forge.dc.modules.dictionary.mapper.DictMapper;
import com.forge.dc.modules.dictionary.service.DictionaryService;
import com.forge.dc.modules.dictionary.vo.DictListVo;
import com.forge.dc.modules.dictionary.vo.DictOptionVo;
import com.forge.dc.modules.users.entity.SysPermissionEntity;
import com.forge.dc.modules.users.mapper.RbacMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final RbacMapper rbacMapper;
    private final DictMapper dictMapper;

    @Override
    public List<DictOptionVo> listPermissionCodeDictOptions() {
        List<SysPermissionEntity> list = rbacMapper.findPermissions();
        return list.stream().map(entity ->
                new DictOptionVo(entity.getPermissionName(), entity.getPermissionCode())).toList();
    }

    @Override
    public List<DictOptionVo> listByDictCode(String dictCode) {
        List<SysDictEntity> list = dictMapper.selectByDictCode(dictCode);
        return list.stream().map(entity ->
                new DictOptionVo(entity.getDictLabel(), entity.getDictValue())).toList();
    }

    @Override
    public PageResult<DictListVo> page(DictPageDto dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<SysDictEntity> list = dictMapper.selectPage(dto.getDictCode());
        PageInfo<SysDictEntity> pageInfo = new PageInfo<>(list);

        List<DictListVo> voList = pageInfo.getList().stream().map(entity -> {
            DictListVo vo = new DictListVo();
            vo.setId(entity.getId());
            vo.setDictCode(entity.getDictCode());
            vo.setDictLabel(entity.getDictLabel());
            vo.setDictValue(entity.getDictValue());
            vo.setSortOrder(entity.getSortOrder());
            vo.setStatus(entity.getStatus());
            vo.setCreatedAt(entity.getCreatedAt());
            return vo;
        }).toList();

        return new PageResult<>(
                pageInfo.getTotal(),
                voList,
                pageInfo.getPageNum(),
                pageInfo.getPageSize()
        );
    }

    @Override
    public void save(DictSaveDto dto) {
        SysDictEntity entity = new SysDictEntity();
        entity.setId(dto.getId());
        entity.setDictCode(dto.getDictCode());
        entity.setDictLabel(dto.getDictLabel());
        entity.setDictValue(dto.getDictValue());
        entity.setSortOrder(dto.getSortOrder());
        entity.setStatus(dto.getStatus());

        int rows;
        if (entity.getId() == null) {
            rows = dictMapper.insert(entity);
        } else {
            rows = dictMapper.update(entity);
        }

        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "save dict failed");
        }
    }

    @Override
    public void removeById(Long id) {
        dictMapper.deleteById(id);
    }
}
