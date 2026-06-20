package com.forge.dc.modules.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.common.util.MinioUtil;
import com.forge.dc.modules.ai.dto.TaskMessage;
import com.forge.dc.modules.ai.dto.TaskPageDTO;
import com.forge.dc.modules.ai.dto.TaskSubmitDTO;
import com.forge.dc.modules.ai.entity.AiTaskEntity;
import com.forge.dc.modules.ai.mapper.AiImageHistoryMapper;
import com.forge.dc.modules.ai.mapper.AiTaskMapper;
import com.forge.dc.modules.ai.service.TaskProducer;
import com.forge.dc.modules.ai.service.TaskService;
import com.forge.dc.modules.ai.vo.TaskVO;
import com.forge.dc.security.LoginUser;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final AiTaskMapper taskMapper;
    private final AiImageHistoryMapper imageHistoryMapper;
    private final TaskProducer taskProducer;
    private final MinioUtil minioUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TaskVO submit(TaskSubmitDTO dto) {
        Long userId = getCurrentUserId();

        if ("img2img".equals(dto.getType())) {
            if (dto.getImages() == null || dto.getImages().isEmpty()
                    || dto.getImages().stream().allMatch(s -> s == null || s.isBlank())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图生图必须提供输入图片");
            }
        }

        AiTaskEntity entity = new AiTaskEntity();
        entity.setUserId(userId);
        entity.setType(dto.getType());
        entity.setStatus("PENDING");
        entity.setPrompt(dto.getPrompt());
        entity.setSize(dto.getSize());
        entity.setRetryCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                entity.setImages(objectMapper.writeValueAsString(dto.getImages()));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "序列化 images 失败");
            }
        }

        taskMapper.insert(entity);
        log.info("任务已创建: taskId={}, type={}", entity.getId(), entity.getType());

        TaskMessage message = new TaskMessage();
        message.setTaskId(entity.getId());
        message.setUserId(userId);
        message.setType(dto.getType());
        message.setPrompt(dto.getPrompt());
        message.setSize(dto.getSize());
        message.setImages(dto.getImages());
        message.setRetryCount(0);

        taskProducer.sendTask(message);

        return toVO(entity);
    }

    @Override
    public TaskVO getTask(Long taskId) {
        Long userId = getCurrentUserId();

        AiTaskEntity entity = taskMapper.selectByIdAndUserId(taskId, userId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "任务不存在");
        }

        return toVO(entity);
    }

    @Override
    public PageResult<TaskVO> page(TaskPageDTO dto) {
        Long userId = getCurrentUserId();

        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<AiTaskEntity> list = taskMapper.selectPage(userId, dto.getStatus());
        PageInfo<AiTaskEntity> pageInfo = new PageInfo<>(list);

        List<TaskVO> voList = pageInfo.getList().stream()
                .map(this::toVO)
                .toList();

        return new PageResult<>(pageInfo.getTotal(), voList, pageInfo.getPageNum(), pageInfo.getPageSize());
    }

    @Override
    public void delete(Long taskId) {
        Long userId = getCurrentUserId();

        AiTaskEntity entity = taskMapper.selectByIdAndUserId(taskId, userId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "任务不存在");
        }

        // 删除 MinIO 上的图片文件（仅当无历史记录引用时）
        if (entity.getObjectName() != null && !entity.getObjectName().isEmpty()) {
            int refCount = imageHistoryMapper.countByObjectName(entity.getObjectName(), userId);
            if (refCount == 0) {
                try {
                    minioUtil.delete(entity.getObjectName());
                    log.info("已删除 MinIO 文件: {}", entity.getObjectName());
                } catch (Exception e) {
                    log.warn("删除 MinIO 文件失败: {}, 继续删除数据库记录", entity.getObjectName(), e);
                }
            } else {
                log.info("MinIO 文件被 {} 条历史记录引用，保留: {}", refCount, entity.getObjectName());
            }
        }

        taskMapper.deleteById(taskId);
        log.info("任务已删除: taskId={}", taskId);
    }

    private TaskVO toVO(AiTaskEntity entity) {
        TaskVO vo = new TaskVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setStatus(entity.getStatus());
        vo.setPrompt(entity.getPrompt());
        vo.setSize(entity.getSize());
        vo.setObjectName(entity.getObjectName());
        vo.setRevisedPrompt(entity.getRevisedPrompt());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getObjectName() != null) {
            vo.setImageUrl(minioUtil.getUrl(entity.getObjectName()));
        }

        return vo;
    }

    private Long getCurrentUserId() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return loginUser.getUserId();
    }
}
