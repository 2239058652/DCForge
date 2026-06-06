package com.forge.dc.modules.Interface.mapper;

import com.forge.dc.modules.Interface.entity.InterfacePermission;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InterfacePermissionMapper {

    @Select("SELECT id, http_method, url_pattern, permission_code, type, description, created_at " +
            "FROM interface_permission")
    List<InterfacePermission> listAll();

    @Insert("INSERT INTO interface_permission (http_method, url_pattern, permission_code, type, description) " +
            "VALUES (#{httpMethod}, #{urlPattern}, #{permissionCode}, #{type}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterfacePermission permission);

    @Update("UPDATE interface_permission SET http_method=#{httpMethod}, url_pattern=#{urlPattern}, " +
            "permission_code=#{permissionCode}, type=#{type}, description=#{description} WHERE id=#{id}")
    int update(InterfacePermission permission);

    @Delete("DELETE FROM interface_permission WHERE id=#{id}")
    void deleteById(Long id);

    List<InterfacePermission> findInterfacePageByCondition(String name, String type);
}