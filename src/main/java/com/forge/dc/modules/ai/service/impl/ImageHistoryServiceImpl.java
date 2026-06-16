package com.forge.dc.modules.ai.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.common.util.MinioUtil;
import com.forge.dc.modules.ai.dto.ImageHistoryPageDTO;
import com.forge.dc.modules.ai.dto.ImageHistorySaveDTO;
import com.forge.dc.modules.ai.entity.AiImageHistoryEntity;
import com.forge.dc.modules.ai.mapper.AiImageHistoryMapper;
import com.forge.dc.modules.ai.service.ImageHistoryService;
import com.forge.dc.modules.ai.vo.ImageHistoryVO;
import com.forge.dc.security.LoginUser;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageHistoryServiceImpl implements ImageHistoryService {

    private final AiImageHistoryMapper imageHistoryMapper;
    private final MinioUtil minioUtil;

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    @Transactional
    public ImageHistoryVO save(ImageHistorySaveDTO dto) {
        Long userId = getCurrentUserId();

        byte[] imageBytes = downloadImage(dto.getImageUrl());
        String ext = inferExtension(dto.getImageUrl(), "png");
        String contentType = "image/" + ext.replace(".", "");
        String objectName = minioUtil.uploadBytes(imageBytes, "ai-image", contentType, ext);
        log.info("图片已上传到 MinIO: {}", objectName);

        AiImageHistoryEntity entity = new AiImageHistoryEntity();
        entity.setUserId(userId);
        entity.setType(dto.getType());
        entity.setPrompt(dto.getPrompt());
        entity.setRevisedPrompt(dto.getRevisedPrompt());
        entity.setObjectName(objectName);
        entity.setSize(dto.getSize());
        entity.setCreatedAt(LocalDateTime.now());

        if (dto.getSourceImageUrls() != null && !dto.getSourceImageUrls().isEmpty()) {
            entity.setSourceImageUrl(String.join(",", dto.getSourceImageUrls()));
        }

        imageHistoryMapper.insert(entity);

        String imageUrl = minioUtil.getUrl(objectName);
        return ImageHistoryVO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .prompt(entity.getPrompt())
                .revisedPrompt(entity.getRevisedPrompt())
                .sourceImageUrl(entity.getSourceImageUrl())
                .imageUrl(imageUrl)
                .size(entity.getSize())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public PageResult<ImageHistoryVO> page(ImageHistoryPageDTO dto) {
        Long userId = getCurrentUserId();

        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<AiImageHistoryEntity> list = imageHistoryMapper.selectPage(userId, dto.getType());
        PageInfo<AiImageHistoryEntity> pageInfo = new PageInfo<>(list);

        List<ImageHistoryVO> voList = pageInfo.getList().stream()
                .map(this::toVO)
                .toList();

        return new PageResult<>(pageInfo.getTotal(), voList, pageInfo.getPageNum(), pageInfo.getPageSize());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long userId = getCurrentUserId();

        AiImageHistoryEntity entity = imageHistoryMapper.selectByIdAndUserId(id, userId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "历史记录不存在");
        }

        try {
            minioUtil.delete(entity.getObjectName());
        } catch (Exception e) {
            log.warn("删除 MinIO 文件失败: {}, 继续删除数据库记录", entity.getObjectName(), e);
        }

        imageHistoryMapper.deleteByIdAndUserId(id, userId);
    }

    private ImageHistoryVO toVO(AiImageHistoryEntity entity) {
        String imageUrl = minioUtil.getUrl(entity.getObjectName());
        return ImageHistoryVO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .prompt(entity.getPrompt())
                .revisedPrompt(entity.getRevisedPrompt())
                .sourceImageUrl(entity.getSourceImageUrl())
                .imageUrl(imageUrl)
                .size(entity.getSize())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Long getCurrentUserId() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return loginUser.getUserId();
    }

    private byte[] downloadImage(String imageUrl) {
        validateUrl(imageUrl);
        Request request = new Request.Builder().url(imageUrl).build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "下载图片失败: HTTP " + response.code());
            }
            return response.body().bytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "下载图片失败: " + e.getMessage());
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "URL 不能为空");
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "URL 格式不合法");
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 HTTP/HTTPS 协议");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "URL 缺少主机名");
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "禁止访问内网地址");
            }
        } catch (UnknownHostException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "无法解析主机名");
        }
    }

    private String inferExtension(String url, String defaultExt) {
        if (url == null) return "." + defaultExt;
        String path = url.split("\\?")[0];
        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1 && lastDot > path.lastIndexOf('/')) {
            String ext = path.substring(lastDot);
            if (ext.matches("\\.(png|jpg|jpeg|webp|gif)")) {
                return ext;
            }
        }
        return "." + defaultExt;
    }
}
