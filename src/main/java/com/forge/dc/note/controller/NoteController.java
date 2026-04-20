package com.forge.dc.note.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.note.service.NoteService;
import com.forge.dc.note.vo.NoteListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
