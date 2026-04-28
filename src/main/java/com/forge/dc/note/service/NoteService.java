package com.forge.dc.note.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.note.dto.NoteAddDto;
import com.forge.dc.note.dto.NotePageDto;
import com.forge.dc.note.vo.NoteListVo;

import java.util.List;

public interface NoteService {

    List<NoteListVo> findNotesAll();

    void addNote(NoteAddDto noteAddDto);

    void deleteNote(Long id);

    void editNote(Long id, NoteAddDto noteAddDto);

    NoteListVo findNoteById(Long id);

    PageResult<NoteListVo> findNotesByPage(NotePageDto notePageDto);
}
