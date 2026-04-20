package com.forge.dc.note.service;

import com.forge.dc.note.vo.NoteListVo;

import java.util.List;

public interface NoteService {

    List<NoteListVo> findNotesAll();

}
