package com.forge.dc.modules.claude.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.modules.claude.dto.ClaudeProjectPageDto;
import com.forge.dc.modules.claude.service.ClaudeService;
import com.forge.dc.modules.claude.vo.ClaudeConversationDetailVo;
import com.forge.dc.modules.claude.vo.ClaudeConversationListVo;
import com.forge.dc.modules.claude.vo.ClaudeProjectVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/claude")
@RequiredArgsConstructor
@Tag(name = "Claude管理", description = "扫描和管理Claude Code本地数据")
public class ClaudeController {

    private final ClaudeService claudeService;

    @GetMapping("/projects")
    @Operation(summary = "分页列出所有Claude项目")
    public Result<PageResult<ClaudeProjectVo>> listProjects(@Valid ClaudeProjectPageDto dto) {
        return Result.success(claudeService.listProjects(dto));
    }

    @GetMapping("/projects/{projectDirName:.+}/conversations")
    @Operation(summary = "列出指定项目下的所有对话会话")
    public Result<List<ClaudeConversationListVo>> listConversations(
            @PathVariable String projectDirName) {
        return Result.success(claudeService.listConversations(projectDirName));
    }

    @GetMapping("/projects/{projectDirName:.+}/conversations/{sessionId:.+}")
    @Operation(summary = "查看指定对话的完整内容")
    public Result<ClaudeConversationDetailVo> getConversationDetail(
            @PathVariable String projectDirName,
            @PathVariable String sessionId) {
        return Result.success(claudeService.getConversationDetail(projectDirName, sessionId));
    }

    @DeleteMapping("/projects/{projectDirName:.+}/conversations/{sessionId:.+}")
    @Operation(summary = "删除指定对话会话")
    public Result<Void> deleteConversation(
            @PathVariable String projectDirName,
            @PathVariable String sessionId) {
        claudeService.deleteConversation(projectDirName, sessionId);
        return Result.success();
    }

    @DeleteMapping("/projects/{projectDirName:.+}")
    @Operation(summary = "删除指定项目（所有对话数据）")
    public Result<Void> deleteProject(@PathVariable String projectDirName) {
        claudeService.deleteProject(projectDirName);
        return Result.success();
    }
}
