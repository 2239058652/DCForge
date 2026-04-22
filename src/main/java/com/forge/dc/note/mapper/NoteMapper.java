package com.forge.dc.note.mapper;

import com.forge.dc.note.entity.NoteEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NoteMapper {

    List<NoteEntity> getNoteList();

    int addNote(NoteEntity noteEntity);

}
