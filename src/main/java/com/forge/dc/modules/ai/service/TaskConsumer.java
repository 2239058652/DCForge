package com.forge.dc.modules.ai.service;

import com.forge.dc.modules.ai.client.AgnesApiClient;
import com.forge.dc.modules.ai.config.RabbitMQConfig;
import com.forge.dc.modules.ai.dto.TaskMessage;
import com.forge.dc.modules.ai.entity.AiTaskEntity;
import com.forge.dc.modules.ai.mapper.AiTaskMapper;
import com.forge.dc.modules.ai.websocket.TaskWebSocketHandler;
import com.forge.dc.modules.ai.websocket.TaskWebSocketHandler.TaskNotification;
import com.forge.dc.common.util.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskConsumer {

    private final AiTaskMapper taskMapper;
    private final AgnesApiClient agnesApiClient;
    private final MinioUtil minioUtil;
    private final TaskWebSocketHandler webSocketHandler;

    public TaskConsumer(AiTaskMapper taskMapper, AgnesApiClient agnesApiClient,
                        MinioUtil minioUtil, TaskWebSocketHandler webSocketHandler) {
        this.taskMapper = taskMapper;
        this.agnesApiClient = agnesApiClient;
        this.minioUtil = minioUtil;
        this.webSocketHandler = webSocketHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consume(TaskMessage message) {
        Long taskId = message.getTaskId();
        log.info("开始处理任务: taskId={}", taskId);

        try {
            // 1. 更新状态为 PROCESSING
            taskMapper.updateStatus(taskId, "PROCESSING", LocalDateTime.now());
            notifyFrontend(taskId, "PROCESSING", null, null, null);

            // 2. 调用 AI 生成图片
            AgnesApiClient.AgnesImageResponse response;
            if ("text2img".equals(message.getType())) {
                response = agnesApiClient.generateImage(message.getPrompt(), message.getSize(), true);
            } else {
                // 图生图：兼容 URL 和 base64 两种格式
                List<String> base64Images = message.getImages().stream()
                        .map(this::toBase64)
                        .collect(Collectors.toList());
                response = agnesApiClient.imageToImage(message.getPrompt(), message.getSize(), base64Images, true);
            }

            // 3. 获取生成结果
            AgnesApiClient.ImageData imageData = response.getData().get(0);
            String imageUrl = imageData.getUrl();
            String revisedPrompt = imageData.getRevisedPrompt();

            // 4. 下载图片并上传到 MinIO
            byte[] imageBytes = downloadFromUrl(imageUrl);
            String ext = inferExtension(imageUrl);
            String contentType = "image/" + ext.replace(".", "");
            String objectName = minioUtil.uploadBytes(imageBytes, "ai-image", contentType, ext);

            // 5. 更新任务为 COMPLETED
            taskMapper.updateResult(taskId, objectName, revisedPrompt, LocalDateTime.now());

            // 6. 生成 MinIO 访问 URL，推送给前端
            String minioUrl = minioUtil.getUrl(objectName);
            notifyFrontend(taskId, "COMPLETED", minioUrl, revisedPrompt, null);

            log.info("任务处理完成: taskId={}, objectName={}", taskId, objectName);

        } catch (Exception e) {
            log.error("任务处理失败: taskId={}", taskId, e);
            handleFailure(taskId, message, e);
        }
    }

    private void handleFailure(Long taskId, TaskMessage message, Exception e) {
        AiTaskEntity task = taskMapper.selectById(taskId);
        int retryCount = task != null ? task.getRetryCount() : 0;

        if (retryCount < 3) {
            taskMapper.incrementRetryCount(taskId);
            log.info("任务将重试: taskId={}, retryCount={}", taskId, retryCount + 1);
            throw new RuntimeException("任务处理失败，等待重试: " + e.getMessage());
        } else {
            String errorMsg = "重试" + retryCount + "次后失败: " + e.getMessage();
            taskMapper.updateError(taskId, errorMsg, LocalDateTime.now());
            notifyFrontend(taskId, "FAILED", null, null, errorMsg);
            log.error("任务最终失败: taskId={}", taskId);
            throw new AmqpRejectAndDontRequeueException(errorMsg);
        }
    }

    private void notifyFrontend(Long taskId, String status, String imageUrl, String revisedPrompt, String errorMessage) {
        TaskNotification notification = new TaskNotification();
        notification.setTaskId(taskId);
        notification.setStatus(status);
        notification.setImageUrl(imageUrl);
        notification.setRevisedPrompt(revisedPrompt);
        notification.setErrorMessage(errorMessage);
        webSocketHandler.notifyTaskUpdate(taskId, notification);
    }

    private byte[] downloadFromUrl(String url) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            java.net.http.HttpResponse<byte[]> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new RuntimeException("下载图片失败: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("下载图片失败: " + e.getMessage());
        }
    }

    /**
     * 将图片字符串转为 base64
     * 兼容两种格式：
     * - URL（新格式）：下载后转 base64
     * - base64（旧格式）：直接返回
     */
    private String toBase64(String image) {
        if (image == null || image.isEmpty()) {
            return image;
        }
        // 如果是 URL，下载后转 base64
        if (image.startsWith("http://") || image.startsWith("https://")) {
            byte[] bytes = downloadFromUrl(image);
            return Base64.getEncoder().encodeToString(bytes);
        }
        // 否则认为是 base64，直接返回
        return image;
    }

    private String inferExtension(String url) {
        if (url == null) return ".png";
        String path = url.split("\\?")[0];
        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1 && lastDot > path.lastIndexOf('/')) {
            String ext = path.substring(lastDot);
            if (ext.matches("\\.(png|jpg|jpeg|webp|gif)")) {
                return ext;
            }
        }
        return ".png";
    }
}
