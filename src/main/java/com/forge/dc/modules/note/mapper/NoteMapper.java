package com.forge.dc.modules.note.mapper;

import com.forge.dc.modules.note.dto.NotePageDto;
import com.forge.dc.modules.note.entity.NoteEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper {

    List<NoteEntity> getNoteList(@Param("userId") Long userId);

    int addNote(NoteEntity noteEntity);


    @Delete("delete from note where id = #{id} and user_id = #{userId}")
    int deleteNoteById(@Param("id") Long id, @Param("userId") Long userId);

    NoteEntity getNoteById(@Param("id") Long id, @Param("userId") Long userId);

    int updateNote(NoteEntity noteEntity);

    Long countNotes(@Param("page") NotePageDto notePageDto, @Param("userId") Long userId);

    List<NoteEntity> getNoteListByPage(@Param("page") NotePageDto notePageDto, @Param("userId") Long userId);

}
