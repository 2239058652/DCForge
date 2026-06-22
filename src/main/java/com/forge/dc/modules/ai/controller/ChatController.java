package com.forge.dc.modules.ai.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.modules.ai.dto.ChatCompletionRequestDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionResponseDTO;
import com.forge.dc.modules.ai.service.ChatCompletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI 聊天")
public class ChatController {

    private final ChatCompletionService chatCompletionService;

    @PostMapping("/completion")
    @Operation(summary = "聊天补全（同步）")
    public Result<ChatCompletionResponseDTO> chatCompletion(@RequestBody @Valid ChatCompletionRequestDTO request) {
        try {
            return Result.success(chatCompletionService.chatCompletion(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(ResultCode.BAD_REQUEST, "AI 聊天请求错误: " + e.getMessage());
        } catch (Exception e) {
            return Result.fail(ResultCode.SYSTEM_ERROR, "AI 聊天服务调用失败: " + e.getMessage());
        }
    }
}
