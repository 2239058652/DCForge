package com.forge.dc.modules.note.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.modules.note.dto.NoteAddDto;
import com.forge.dc.modules.note.dto.NotePageDto;
import com.forge.dc.modules.note.dto.NoteUpdateDto;
import com.forge.dc.modules.note.entity.NoteEntity;
import com.forge.dc.modules.note.mapper.NoteMapper;
import com.forge.dc.modules.note.service.NoteService;
import com.forge.dc.modules.note.vo.NoteListVo;
import com.forge.dc.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        List<NoteEntity> noteEntityList = noteMapper.getNoteList(getCurrentUserId());
        return noteEntityList.stream().map(this::toNoteListVo).toList();
    }

    @Override
    public void addNote(NoteAddDto noteAddDto) {

        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setUserId(getCurrentUserId());
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
        int rows = noteMapper.deleteNoteById(id, getCurrentUserId());
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "note不存在");
        }
    }

    @Override
    public void editNote(Long id, NoteUpdateDto noteUpdateDto) {
        NoteEntity noteEntity = noteMapper.getNoteById(id, getCurrentUserId());
        if (noteEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "note不存在");
        }

        noteEntity.setContent(noteUpdateDto.getContent());
        noteEntity.setUpdatedAt(LocalDateTime.now());
        int rows = noteMapper.updateNote(noteEntity);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "更新note失败");
        }
    }

    @Override
    public NoteListVo findNoteById(Long id) {
        NoteEntity noteEntity = noteMapper.getNoteById(id, getCurrentUserId());
        if (noteEntity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "note不存在");
        }

        return toNoteListVo(noteEntity);
    }

    @Override
    public PageResult<NoteListVo> findNotesByPage(NotePageDto notePageDto) {
        Long userId = getCurrentUserId();
        // 清洗 content 字段：trim 后为空则设为 null
        String content = notePageDto.getContent();
        if (content != null) {
            content = content.trim();          // 去除首尾空格
            if (content.isEmpty()) {
                content = null;                // 空字符串或纯空格置为 null
            }
            notePageDto.setContent(content);
        }

        Long total = noteMapper.countNotes(notePageDto, userId);

        Integer offset = (notePageDto.getPageNum() - 1) * notePageDto.getPageSize();
        notePageDto.setOffset(offset);

        List<NoteEntity> noteEntityList = noteMapper.getNoteListByPage(notePageDto, userId);
        List<NoteListVo> noteListVoList = noteEntityList.stream().map(this::toNoteListVo).toList();

        return new PageResult<>(total, noteListVoList, notePageDto.getPageNum(), notePageDto.getPageSize());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "unauthorized");
        }
        return loginUser.getUserId();
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
