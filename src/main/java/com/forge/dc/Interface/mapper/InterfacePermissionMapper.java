package com.forge.dc.Interface.mapper;

import com.forge.dc.Interface.entity.InterfacePermission;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InterfacePermissionMapper {

    @Select("SELECT id, http_method, url_pattern, permission_code, description, created_at " +
            "FROM interface_permission")
    List<InterfacePermission> listAll();

    @Insert("INSERT INTO interface_permission (http_method, url_pattern, permission_code, description) " +
            "VALUES (#{httpMethod}, #{urlPattern}, #{permissionCode}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterfacePermission permission);

    @Update("UPDATE interface_permission SET http_method=#{httpMethod}, url_pattern=#{urlPattern}, " +
            "permission_code=#{permissionCode}, description=#{description} WHERE id=#{id}")
    int update(InterfacePermission permission);

    @Delete("DELETE FROM interface_permission WHERE id=#{id}")
    int deleteById(Long id);
}