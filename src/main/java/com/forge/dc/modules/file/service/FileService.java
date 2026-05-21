package com.forge.dc.modules.file.service;

import com.forge.dc.modules.file.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadVO upload(MultipartFile file, String prefix);

    String getUrl(String objectName);

    void delete(String objectName);
}