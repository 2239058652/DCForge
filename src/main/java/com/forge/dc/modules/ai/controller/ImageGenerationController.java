package com.forge.dc.modules.ai.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.modules.ai.dto.ImageToImageDTO;
import com.forge.dc.modules.ai.dto.TextToImageDTO;
import com.forge.dc.modules.ai.service.ImageGenerationService;
import com.forge.dc.modules.ai.vo.ImageGenerationVO;
import com.forge.dc.common.util.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai/image")
@RequiredArgsConstructor
@Tag(name = "AI 图像生成")
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;
    private final MinioUtil minioUtil;

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

    @PostMapping("/upload")
    @Operation(summary = "上传图片到 MinIO")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        String objectName = minioUtil.uploadTemp(file);
        String url = minioUtil.getUrl(objectName);
        return Result.success(null, url);
    }
}
