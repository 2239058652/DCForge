package com.forge.dc.modules.dictionary.mapper;

import com.forge.dc.modules.dictionary.entity.SysDictEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictMapper {

    List<SysDictEntity> selectByDictCode(@Param("dictCode") String dictCode);

    int insert(SysDictEntity entity);

    int update(SysDictEntity entity);

    int deleteById(@Param("id") Long id);

    List<SysDictEntity> selectPage(@Param("dictCode") String dictCode);
}
