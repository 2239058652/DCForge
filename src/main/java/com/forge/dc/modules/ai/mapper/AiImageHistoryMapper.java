package com.forge.dc.modules.ai.mapper;

import com.forge.dc.modules.ai.entity.AiImageHistoryEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiImageHistoryMapper {

    @Insert("INSERT INTO ai_image_history (user_id, type, prompt, revised_prompt, source_image_url, object_name, size, created_at) " +
            "VALUES (#{userId}, #{type}, #{prompt}, #{revisedPrompt}, #{sourceImageUrl}, #{objectName}, #{size}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiImageHistoryEntity entity);

    List<AiImageHistoryEntity> selectPage(@Param("userId") Long userId, @Param("type") String type);

    @Select("SELECT * FROM ai_image_history WHERE id = #{id} AND user_id = #{userId}")
    AiImageHistoryEntity selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM ai_image_history WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
