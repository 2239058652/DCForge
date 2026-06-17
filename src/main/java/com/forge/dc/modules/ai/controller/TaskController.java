package com.forge.dc.modules.ai.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.modules.ai.dto.TaskPageDTO;
import com.forge.dc.modules.ai.dto.TaskSubmitDTO;
import com.forge.dc.modules.ai.service.TaskService;
import com.forge.dc.modules.ai.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/task")
@RequiredArgsConstructor
@Tag(name = "AI 任务管理", description = "异步任务提交、查询")
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/submit")
    @Operation(summary = "提交生成任务")
    public Result<TaskVO> submit(@RequestBody @Valid TaskSubmitDTO dto) {
        return Result.success(taskService.submit(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询任务状态")
    public Result<TaskVO> getTask(@PathVariable Long id) {
        return Result.success(taskService.getTask(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询任务列表")
    public Result<PageResult<TaskVO>> page(@Valid TaskPageDTO dto) {
        return Result.success(taskService.page(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务记录")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.success();
    }
}
