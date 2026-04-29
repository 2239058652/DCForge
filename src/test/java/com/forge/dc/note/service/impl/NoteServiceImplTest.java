package com.forge.dc.note.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.note.dto.NoteAddDto;
import com.forge.dc.note.dto.NotePageDto;
import com.forge.dc.note.dto.NoteUpdateDto;
import com.forge.dc.note.service.NoteService;
import com.forge.dc.note.vo.NoteListVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class NoteServiceImplTest {

    @Autowired
    private NoteService noteService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanNoteTable();
    }

    @AfterEach
    void tearDown() {
        cleanNoteTable();
    }

    @Test
    void findNotesAll_shouldReturnNotesFromDatabase() {
        insertNote("first content");
        insertNote("second content");

        List<NoteListVo> result = noteService.findNotesAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("second content");
        assertThat(result.get(1).getContent()).isEqualTo("first content");
        assertThat(result.get(0).getId()).isNotNull();
        assertThat(result.get(0).getCreatedAt()).isNotNull();
        assertThat(result.get(0).getUpdatedAt()).isNotNull();
    }

    @Test
    void addNote_shouldSaveNoteToDatabase() {
        NoteAddDto dto = new NoteAddDto();
        dto.setContent("new content");

        noteService.addNote(dto);

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from note where content = ?",
                Integer.class,
                "new content"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleteNote_shouldDeleteNoteFromDatabase() {
        Long id = insertNote("delete content");

        noteService.deleteNote(id);

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from note where id = ?",
                Integer.class,
                id
        );
        assertThat(count).isZero();
    }

    @Test
    void deleteNote_shouldThrowBusinessExceptionWhenNoteNotExists() {
        assertThatThrownBy(() -> noteService.deleteNote(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
    }

    @Test
    void editNote_shouldUpdateNoteInDatabase() {
        Long id = insertNote("old content");
        NoteUpdateDto dto = new NoteUpdateDto();
        dto.setContent("updated content");

        noteService.editNote(id, dto);

        String content = jdbcTemplate.queryForObject(
                "select content from note where id = ?",
                String.class,
                id
        );
        assertThat(content).isEqualTo("updated content");
    }

    @Test
    void editNote_shouldThrowBusinessExceptionWhenNoteNotFound() {
        NoteUpdateDto dto = new NoteUpdateDto();
        dto.setContent("updated content");

        assertThatThrownBy(() -> noteService.editNote(-1L, dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void findNoteById_shouldReturnNoteFromDatabase() {
        Long id = insertNote("find content");

        NoteListVo result = noteService.findNoteById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getContent()).isEqualTo("find content");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void findNoteById_shouldThrowBusinessExceptionWhenNoteNotFound() {
        assertThatThrownBy(() -> noteService.findNoteById(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void findNotesByPage_shouldTrimContentAndReturnMatchedNotes() {
        insertNote("java note");
        insertNote("spring note");
        insertNote("mysql record");

        NotePageDto dto = new NotePageDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        dto.setContent("  note  ");

        PageResult<NoteListVo> result = noteService.findNotesByPage(dto);

        assertThat(dto.getContent()).isEqualTo("note");
        assertThat(dto.getOffset()).isZero();
        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getRecords()).extracting(NoteListVo::getContent)
                .containsExactly("spring note", "java note");
    }

    @Test
    void findNotesByPage_shouldSetBlankContentToNullAndReturnAllNotes() {
        insertNote("first content");
        insertNote("second content");

        NotePageDto dto = new NotePageDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        dto.setContent("   ");

        PageResult<NoteListVo> result = noteService.findNotesByPage(dto);

        assertThat(dto.getContent()).isNull();
        assertThat(dto.getOffset()).isZero();
        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getRecords()).hasSize(2);
    }

    private Long insertNote(String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into note (content, created_at, updated_at) values (?, now(), now())",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, content);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void cleanNoteTable() {
        jdbcTemplate.update("delete from note");
    }
}
