package com.forge.dc.note.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.note.dto.NoteAddDto;
import com.forge.dc.note.entity.NoteEntity;
import com.forge.dc.note.mapper.NoteMapper;
import com.forge.dc.note.service.NoteService;
import com.forge.dc.note.vo.NoteListVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Override
    public void addNote(NoteAddDto note) {

        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setContent(note.getContent());
        noteEntity.setCreatedAt(LocalDateTime.now());
        noteEntity.setUpdatedAt(LocalDateTime.now());
        int rows = noteMapper.addNote(noteEntity);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "新增note失败");
        }
    }
}
