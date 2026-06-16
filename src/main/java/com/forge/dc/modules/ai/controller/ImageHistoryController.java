package com.forge.dc.modules.ai.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.modules.ai.dto.ImageHistoryPageDTO;
import com.forge.dc.modules.ai.dto.ImageHistorySaveDTO;
import com.forge.dc.modules.ai.service.ImageHistoryService;
import com.forge.dc.modules.ai.vo.ImageHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/image/history")
@RequiredArgsConstructor
@Tag(name = "AI 图片历史", description = "AI 生成图片的历史记录管理")
public class ImageHistoryController {

    private final ImageHistoryService imageHistoryService;

    @PostMapping("/save")
    @Operation(summary = "保存图片到 MinIO 并记录历史")
    public Result<ImageHistoryVO> save(@RequestBody @Valid ImageHistorySaveDTO dto) {
        return Result.success(imageHistoryService.save(dto));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询历史记录")
    public Result<PageResult<ImageHistoryVO>> page(@Valid ImageHistoryPageDTO dto) {
        return Result.success(imageHistoryService.page(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除历史记录")
    public Result<Void> delete(@PathVariable Long id) {
        imageHistoryService.delete(id);
        return Result.success();
    }
}
