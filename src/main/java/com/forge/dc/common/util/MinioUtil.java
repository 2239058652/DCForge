package com.forge.dc.common.util;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /**
     * 上传文件，返回对象名（objectName）
     * objectName 格式：prefix/uuid.ext，例如 "avatar/abc123.jpg"
     *
     * @param file   MultipartFile，直接接 Controller 传来的参数
     * @param prefix 存储目录前缀，如 "avatar"、"post"
     * @return objectName，存入数据库，后续用来生成访问链接或删除
     */
    public String upload(MultipartFile file, String prefix) {
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String objectName = prefix + "/" + UUID.randomUUID() + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
        return objectName;
    }

    /**
     * 生成预签名访问 URL（有效期 7 天）
     * 前端用这个 URL 直接访问/显示文件
     *
     * @param objectName upload() 返回的 objectName
     * @return 可直接访问的临时 URL
     */
    public String getUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("生成访问链接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从字节数组上传文件，返回对象名（objectName）
     * 适用于从 URL 下载图片后再上传的场景
     *
     * @param data        文件字节数组
     * @param prefix      存储目录前缀
     * @param contentType 文件类型，如 "image/png"
     * @param ext         文件扩展名，如 ".png"
     * @return objectName
     */
    public String uploadBytes(byte[] data, String prefix, String contentType, String ext) {
        String objectName = prefix + "/" + UUID.randomUUID() + ext;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
        return objectName;
    }

    /**
     * 删除文件
     *
     * @param objectName upload() 返回的 objectName
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传临时文件到 temp-image 目录
     * 用于图生图上传，定时清理
     */
    public String uploadTemp(MultipartFile file) {
        return upload(file, "temp-image");
    }

    /**
     * 清理 temp-image 下超过指定时间的文件
     *
     * @param duration 超过此时间的文件会被删除
     */
    public void deleteTempOlderThan(Duration duration) {
        try {
            Instant cutoff = Instant.now().minus(duration);
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix("temp-image/")
                    .recursive(true)
                    .build();

            Iterable<Result<Item>> results = minioClient.listObjects(args);
            int count = 0;
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir() && item.lastModified().toInstant().isBefore(cutoff)) {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.objectName())
                            .build());
                    count++;
                }
            }
            if (count > 0) {
                log.info("已清理 {} 个过期临时文件", count);
            }
        } catch (Exception e) {
            log.warn("清理临时文件失败", e);
        }
    }
}