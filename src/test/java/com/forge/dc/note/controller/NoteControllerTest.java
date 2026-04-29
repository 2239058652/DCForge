package com.forge.dc.note.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.note.dto.NoteAddDto;
import com.forge.dc.note.dto.NoteUpdateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void findAllNotesList_shouldReturnNotesFromDatabase() throws Exception {
        insertNote("first content");
        insertNote("second content");

        mockMvc.perform(get("/notes/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].content").value("second content"))
                .andExpect(jsonPath("$.data[1].content").value("first content"));
    }

    @Test
    void addNote_shouldSaveNoteToDatabase() throws Exception {
        NoteAddDto dto = new NoteAddDto();
        dto.setContent("new content");

        mockMvc.perform(post("/notes/add")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from note where content = ?",
                Integer.class,
                "new content"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void addNote_shouldReturnBadRequestWhenContentIsBlank() throws Exception {
        NoteAddDto dto = new NoteAddDto();
        dto.setContent("");

        mockMvc.perform(post("/notes/add")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        Integer count = jdbcTemplate.queryForObject("select count(*) from note", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void deleteNote_shouldDeleteNoteFromDatabase() throws Exception {
        Long id = insertNote("delete content");

        mockMvc.perform(delete("/notes/delete/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from note where id = ?",
                Integer.class,
                id
        );
        assertThat(count).isZero();
    }

    @Test
    void findNoteById_shouldReturnNoteFromDatabase() throws Exception {
        Long id = insertNote("find content");

        mockMvc.perform(get("/notes/find/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.content").value("find content"));
    }

    @Test
    void updateNote_shouldUpdateNoteInDatabase() throws Exception {
        Long id = insertNote("old content");
        NoteUpdateDto dto = new NoteUpdateDto();
        dto.setContent("updated content");

        mockMvc.perform(put("/notes/update/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));

        String content = jdbcTemplate.queryForObject(
                "select content from note where id = ?",
                String.class,
                id
        );
        assertThat(content).isEqualTo("updated content");
    }

    @Test
    void updateNote_shouldReturnBadRequestWhenContentIsBlank() throws Exception {
        Long id = insertNote("old content");
        NoteUpdateDto dto = new NoteUpdateDto();
        dto.setContent("");

        mockMvc.perform(put("/notes/update/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        String content = jdbcTemplate.queryForObject(
                "select content from note where id = ?",
                String.class,
                id
        );
        assertThat(content).isEqualTo("old content");
    }

    @Test
    void findNotesByPage_shouldReturnPagedNotesFromDatabase() throws Exception {
        insertNote("java note");
        insertNote("spring note");
        insertNote("mysql record");

        mockMvc.perform(get("/notes/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("content", "note"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.records[0].content").value("spring note"))
                .andExpect(jsonPath("$.data.records[1].content").value("java note"));
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
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void cleanNoteTable() {
        jdbcTemplate.update("delete from note");
    }
}
