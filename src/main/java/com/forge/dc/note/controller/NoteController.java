package com.forge.dc.note.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.note.dto.NoteAddDto;
import com.forge.dc.note.dto.NotePageDto;
import com.forge.dc.note.service.NoteService;
import com.forge.dc.note.vo.NoteListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notes")
@Tag(name = "note管理", description = "note的增删改查相关接口")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * 查询note列表
     *
     */
    @Operation(summary = "查询note列表")
    @GetMapping("/list")
    public Result<List<NoteListVo>> findAllNotesList() {
        return Result.success(noteService.findNotesAll());
    }

    /**
     * 新增note
     *
     */
    @Operation(summary = "新增note")
    @PostMapping("/add")
    public Result<Void> addNote(@RequestBody @Valid NoteAddDto noteAddDto) {
        noteService.addNote(noteAddDto);
        return Result.success();
    }

    /**
     * 删除note
     *
     */
    @Operation(summary = "删除note")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return Result.success();
    }

    /**
     * 根据id查询note信息
     *
     */
    @Operation(summary = "根据id查询note信息")
    @GetMapping("/find/{id}")
    public Result<NoteListVo> findNoteById(@PathVariable Long id) {
        return Result.success(noteService.findNoteById(id));
    }

    /**
     * 修改note
     *
     */
    @Operation(summary = "修改note")
    @PutMapping("/update/{id}")
    public Result<Void> updateNote(@PathVariable Long id, @RequestBody @Valid NoteAddDto noteAddDto) {
        noteService.editNote(id, noteAddDto);
        return Result.success();
    }

    /**
     * 分页查询note列表
     *
     */
    @Operation(summary = "分页查询note列表")
    @GetMapping("/page")
    public Result<PageResult<NoteListVo>> findNotesByPage(@Valid NotePageDto notePageDto) {
        return Result.success(noteService.findNotesByPage(notePageDto.getPageNum(), notePageDto.getPageSize()));
    }

}
