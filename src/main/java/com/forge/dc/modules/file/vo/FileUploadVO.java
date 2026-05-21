package com.forge.dc.modules.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadVO {
    /**
     * 存入数据库的字段，后续用来删除或刷新 URL
     */
    private String objectName;
    /**
     * 返回给前端展示用的临时访问链接
     */
    private String url;
}