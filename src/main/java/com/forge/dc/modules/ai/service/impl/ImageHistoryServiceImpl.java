package com.forge.dc.modules.ai.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.common.util.MinioUtil;
import com.forge.dc.modules.ai.dto.ImageHistoryPageDTO;
import com.forge.dc.modules.ai.dto.ImageHistorySaveDTO;
import com.forge.dc.modules.ai.entity.AiImageHistoryEntity;
import com.forge.dc.modules.ai.entity.AiTaskEntity;
import com.forge.dc.modules.ai.mapper.AiImageHistoryMapper;
import com.forge.dc.modules.ai.mapper.AiTaskMapper;
import com.forge.dc.modules.ai.service.ImageHistoryService;
import com.forge.dc.modules.ai.vo.ImageHistoryVO;
import com.forge.dc.security.LoginUser;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageHistoryServiceImpl implements ImageHistoryService {

    private final AiImageHistoryMapper imageHistoryMapper;
    private final AiTaskMapper aiTaskMapper;
    private final MinioUtil minioUtil;

    @Override
    @Transactional
    public ImageHistoryVO save(ImageHistorySaveDTO dto) {
        Long userId = getCurrentUserId();

        // 从 ai_task 表读取任务信息
        AiTaskEntity task = aiTaskMapper.selectByIdAndUserId(dto.getTaskId(), userId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "任务不存在");
        }
        if (!"COMPLETED".equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "任务未完成，无法保存");
        }
        if (task.getObjectName() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "任务无图片数据");
        }

        // 直接复用 objectName，不重复下载上传
        AiImageHistoryEntity entity = new AiImageHistoryEntity();
        entity.setUserId(userId);
        entity.setType(task.getType());
        entity.setPrompt(task.getPrompt());
        entity.setRevisedPrompt(task.getRevisedPrompt());
        entity.setObjectName(task.getObjectName());
        entity.setSize(task.getSize());
        entity.setCreatedAt(LocalDateTime.now());

        if (dto.getSourceImageUrls() != null && !dto.getSourceImageUrls().isEmpty()) {
            // Data URI base64 太长，只保留 URL 部分或截断标记
            List<String> urls = dto.getSourceImageUrls().stream()
                    .map(url -> url.startsWith("data:") ? "[base64 image]" : url)
                    .toList();
            entity.setSourceImageUrl(String.join(",", urls));
        }

        imageHistoryMapper.insert(entity);

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

        // 删除 MinIO 文件（仅当任务不再引用时）
        if (entity.getObjectName() != null && !entity.getObjectName().isEmpty()) {
            AiTaskEntity task = aiTaskMapper.selectByObjectName(entity.getObjectName(), userId);
            if (task == null) {
                try {
                    minioUtil.delete(entity.getObjectName());
                } catch (Exception e) {
                    log.warn("删除 MinIO 文件失败: {}, 继续删除数据库记录", entity.getObjectName(), e);
                }
            } else {
                log.info("任务仍引用 MinIO 文件，保留: {}", entity.getObjectName());
            }
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

}
