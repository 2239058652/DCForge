package com.forge.dc.modules.ai.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.modules.ai.dto.ImageToImageDTO;
import com.forge.dc.modules.ai.dto.TextToImageDTO;
import com.forge.dc.modules.ai.service.ImageGenerationService;
import com.forge.dc.modules.ai.vo.ImageGenerationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/image")
@RequiredArgsConstructor
@Tag(name = "AI 图像生成")
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;

    @PostMapping("/text-to-image")
    @Operation(summary = "文生图")
    public Result<ImageGenerationVO> textToImage(@RequestBody @Valid TextToImageDTO dto) {
        return Result.success(imageGenerationService.textToImage(dto));
    }

    @PostMapping("/image-to-image")
    @Operation(summary = "图生图")
    public Result<ImageGenerationVO> imageToImage(@RequestBody @Valid ImageToImageDTO dto) {
        return Result.success(imageGenerationService.imageToImage(dto));
    }
}
