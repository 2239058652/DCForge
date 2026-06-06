package com.forge.dc.modules.dictionary.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.dictionary.dto.DictPageDto;
import com.forge.dc.modules.dictionary.dto.DictSaveDto;
import com.forge.dc.modules.dictionary.vo.DictListVo;
import com.forge.dc.modules.dictionary.vo.DictOptionVo;

import java.util.List;

public interface DictionaryService {

    List<DictOptionVo> listPermissionCodeDictOptions();

    List<DictOptionVo> listByDictCode(String dictCode);

    PageResult<DictListVo> page(DictPageDto dto);

    void save(DictSaveDto dto);

    void removeById(Long id);
}
