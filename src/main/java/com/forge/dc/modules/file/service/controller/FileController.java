package com.forge.dc.modules.file.service.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.modules.file.service.FileService;
import com.forge.dc.modules.file.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Tag(name = "文件管理")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload/avatar")
    @Operation(summary = "上传头像")
    public Result<FileUploadVO> uploadAvatar(@RequestParam MultipartFile file) {
        return Result.success(fileService.upload(file, "avatar"));
    }

    @PostMapping("/upload/post")
    @Operation(summary = "上传帖子附件")
    public Result<FileUploadVO> uploadPost(@RequestParam MultipartFile file) {
        return Result.success(fileService.upload(file, "post"));
    }

    @GetMapping("/url")
    @Operation(summary = "刷新文件访问链接")
    public Result<String> getUrl(@RequestParam String objectName) {
        return Result.success(fileService.getUrl(objectName));
    }
}