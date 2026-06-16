package com.forge.dc.modules.ai.mapper;

import com.forge.dc.modules.ai.entity.AiTaskEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiTaskMapper {

    @Insert("INSERT INTO ai_task (user_id, type, status, prompt, size, images, created_at, updated_at) " +
            "VALUES (#{userId}, #{type}, #{status}, #{prompt}, #{size}, #{images}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiTaskEntity entity);

    @Update("UPDATE ai_task SET status = #{status}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE ai_task SET status = 'COMPLETED', object_name = #{objectName}, revised_prompt = #{revisedPrompt}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateResult(@Param("id") Long id, @Param("objectName") String objectName, @Param("revisedPrompt") String revisedPrompt, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE ai_task SET status = 'FAILED', error_message = #{errorMessage}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateError(@Param("id") Long id, @Param("errorMessage") String errorMessage, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE ai_task SET retry_count = retry_count + 1 WHERE id = #{id}")
    int incrementRetryCount(@Param("id") Long id);

    @Select("SELECT * FROM ai_task WHERE id = #{id}")
    AiTaskEntity selectById(@Param("id") Long id);

    @Select("SELECT * FROM ai_task WHERE id = #{id} AND user_id = #{userId}")
    AiTaskEntity selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<AiTaskEntity> selectPage(@Param("userId") Long userId, @Param("status") String status);
}
