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

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

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
                List<String> base64Images = new java.util.ArrayList<>();
                for (String img : message.getImages()) {
                    base64Images.add(toBase64(img));
                }
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
        if (!(e instanceof java.io.IOException)) {
            String errorMsg = "请求错误(不可重试): " + e.getMessage();
            taskMapper.updateError(taskId, errorMsg, LocalDateTime.now());
            notifyFrontend(taskId, "FAILED", null, null, errorMsg);
            log.error("任务不可重试失败: taskId={}, error={}", taskId, e.getMessage());
            throw new AmqpRejectAndDontRequeueException(errorMsg);
        }

        AiTaskEntity task = taskMapper.selectById(taskId);
        int retryCount = task != null ? task.getRetryCount() : 0;

        if (retryCount < 3) {
            taskMapper.incrementRetryCount(taskId);
            log.info("任务将重试: taskId={}, retryCount={}, error={}", taskId, retryCount + 1, e.getMessage());
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

    private static final long MAX_DOWNLOAD_SIZE = 50 * 1024 * 1024;

    private byte[] downloadFromUrl(String url) throws IOException {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("不支持的协议: " + scheme);
            }

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(30))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();
            java.net.http.HttpResponse<byte[]> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = response.statusCode();
            if (statusCode >= 500) {
                throw new IOException("服务端错误: HTTP " + statusCode);
            }
            if (statusCode != 200) {
                throw new IllegalStateException("下载图片失败: HTTP " + statusCode);
            }
            byte[] body = response.body();
            if (body.length > MAX_DOWNLOAD_SIZE) {
                throw new IllegalStateException("图片过大: " + body.length + " bytes");
            }
            return body;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("下载图片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将图片字符串转为 Data URI base64
     * 兼容两种格式：
     * - URL（新格式）：下载后转 data:image/...;base64,xxx
     * - base64/Data URI（旧格式）：直接返回
     */
    private String toBase64(String image) throws IOException {
        if (image == null || image.isEmpty()) {
            return image;
        }
        // 如果已经是 Data URI，直接返回
        if (image.startsWith("data:image/")) {
            return image;
        }
        // 如果是 URL，下载后转 Data URI
        if (image.startsWith("http://") || image.startsWith("https://")) {
            byte[] bytes = downloadFromUrl(image);
            String ext = inferExtensionFromBytes(bytes);
            return "data:image/" + ext + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        // 否则认为是裸 base64，添加 Data URI 前缀（默认 png）
        return "data:image/png;base64," + image;
    }

    private String inferExtensionFromBytes(byte[] bytes) {
        if (bytes.length >= 8) {
            // PNG: 89 50 4E 47
            if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "png";
            // JPEG: FF D8 FF
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return "jpeg";
            // WEBP: RIFF....WEBP
            if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46) return "webp";
            // GIF: 47 49 46 38
            if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) return "gif";
        }
        return "png";
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
