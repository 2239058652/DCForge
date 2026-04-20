package com.forge.dc.note.service.impl;

import com.forge.dc.note.entity.NoteEntity;
import com.forge.dc.note.mapper.NoteMapper;
import com.forge.dc.note.service.NoteService;
import com.forge.dc.note.vo.NoteListVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;

    public NoteServiceImpl(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    @Override
    public List<NoteListVo> findNotesAll() {
        List<NoteEntity> noteEntityList = noteMapper.getNoteList();
        return noteEntityList.stream().map(note -> {
            NoteListVo noteListVo = new NoteListVo();
            noteListVo.setId(note.getId());
            noteListVo.setContent(note.getContent());
            noteListVo.setCreatedAt(note.getCreatedAt());
            noteListVo.setUpdatedAt(note.getUpdatedAt());
            return noteListVo;
        }).toList();
    }
}
