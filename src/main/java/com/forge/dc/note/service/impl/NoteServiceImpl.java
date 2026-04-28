package com.forge.dc.note.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
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
        return noteEntityList.stream().map(this::toNoteListVo).toList();
    }

    @Override
    public void addNote(NoteAddDto noteAddDto) {

        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setContent(noteAddDto.getContent());
        noteEntity.setCreatedAt(LocalDateTime.now());
        noteEntity.setUpdatedAt(LocalDateTime.now());

        int rows = noteMapper.addNote(noteEntity);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "新增note失败");
        }
    }

    @Override
    public void deleteNote(Long id) {
        int rows = noteMapper.deleteNoteById(id);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "note不存在");
        }
    }

    @Override
    public void editNote(Long id, NoteAddDto noteAddDto) {
        NoteEntity noteEntity = noteMapper.getNoteById(id);
        if (noteEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "note不存在");
        }

        noteEntity.setContent(noteAddDto.getContent());
        noteEntity.setUpdatedAt(LocalDateTime.now());
        int rows = noteMapper.updateNote(noteEntity);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "更新note失败");
        }
    }

    @Override
    public NoteListVo findNoteById(Long id) {
        NoteEntity noteEntity = noteMapper.getNoteById(id);
        if (noteEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "note不存在");
        }

        return toNoteListVo(noteEntity);
    }

    @Override
    public PageResult<NoteListVo> findNotesByPage(Integer pageNum, Integer pageSize) {
        Long total = noteMapper.countNotes();
        Integer offset = (pageNum - 1) * pageSize;

        List<NoteEntity> noteEntityList = noteMapper.getNoteListByPage(offset, pageSize);
        List<NoteListVo> noteListVoList = noteEntityList.stream().map(this::toNoteListVo).toList();

        return new PageResult<>(total, noteListVoList, pageNum, pageSize);
    }

    private NoteListVo toNoteListVo(NoteEntity noteEntity) {
        NoteListVo noteListVo = new NoteListVo();

        noteListVo.setId(noteEntity.getId());
        noteListVo.setContent(noteEntity.getContent());
        noteListVo.setCreatedAt(noteEntity.getCreatedAt());
        noteListVo.setUpdatedAt(noteEntity.getUpdatedAt());
        return noteListVo;
    }
}
