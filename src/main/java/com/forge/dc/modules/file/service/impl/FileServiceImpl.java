package com.forge.dc.modules.file.service.impl;

import com.forge.dc.common.util.MinioUtil;
import com.forge.dc.modules.file.service.FileService;
import com.forge.dc.modules.file.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioUtil minioUtil;

    @Override
    public FileUploadVO upload(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        // 限制文件大小 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过 10MB");
        }
        String objectName = minioUtil.upload(file, prefix);
        String url = minioUtil.getUrl(objectName);
        return FileUploadVO.builder()
                .objectName(objectName)
                .url(url)
                .build();
    }

    @Override
    public String getUrl(String objectName) {
        return minioUtil.getUrl(objectName);
    }

    @Override
    public void delete(String objectName) {
        minioUtil.delete(objectName);
    }
}