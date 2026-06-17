package com.forge.dc.modules.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.common.util.JwtUtils;
import com.forge.dc.common.util.MinioUtil;
import com.forge.dc.modules.ai.entity.AiTaskEntity;
import com.forge.dc.modules.ai.mapper.AiTaskMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtils jwtUtils;
    private final AiTaskMapper taskMapper;
    private final MinioUtil minioUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> taskSubscribers = new ConcurrentHashMap<>();

    public TaskWebSocketHandler(JwtUtils jwtUtils, AiTaskMapper taskMapper, MinioUtil minioUtil) {
        this.jwtUtils = jwtUtils;
        this.taskMapper = taskMapper;
        this.minioUtil = minioUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        session.getAttributes().put("userId", userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);

        log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到 WebSocket 消息: {}", payload);

        SubscribeMessage msg = objectMapper.readValue(payload, SubscribeMessage.class);

        if (msg.getTaskId() != null) {
            if ("subscribe".equals(msg.getAction())) {
                taskSubscribers.computeIfAbsent(msg.getTaskId(), k -> ConcurrentHashMap.newKeySet()).add(session);
                log.info("用户订阅任务: taskId={}, sessionId={}", msg.getTaskId(), session.getId());

                // 如果任务已完成或失败，立即推送缓存结果
                sendCachedResultIfDone(msg.getTaskId(), session);
            } else if ("unsubscribe".equals(msg.getAction())) {
                Set<WebSocketSession> sessions = taskSubscribers.get(msg.getTaskId());
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        taskSubscribers.remove(msg.getTaskId());
                    }
                }
                log.info("用户取消订阅: taskId={}, sessionId={}", msg.getTaskId(), session.getId());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }

            taskSubscribers.values().forEach(set -> set.remove(session));
        }

        log.info("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    public void notifyTaskUpdate(Long taskId, TaskNotification notification) {
        Set<WebSocketSession> sessions = taskSubscribers.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("没有订阅 taskId={} 的连接", taskId);
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(notification);
        } catch (Exception e) {
            log.error("序列化通知消息失败", e);
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("发送 WebSocket 消息失败: sessionId={}", session.getId(), e);
                }
            }
        }

        log.info("已推送任务更新: taskId={}, status={}, 推送{}个连接", taskId, notification.getStatus(), sessions.size());
    }

    private void sendCachedResultIfDone(Long taskId, WebSocketSession session) {
        AiTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) return;

        if ("COMPLETED".equals(task.getStatus())) {
            String minioUrl = minioUtil.getUrl(task.getObjectName());
            TaskNotification notification = new TaskNotification();
            notification.setTaskId(taskId);
            notification.setStatus("COMPLETED");
            notification.setImageUrl(minioUrl);
            notification.setRevisedPrompt(task.getRevisedPrompt());
            sendToSession(session, notification);
            log.info("subscribe 时推送已完成任务: taskId={}", taskId);
        } else if ("FAILED".equals(task.getStatus())) {
            TaskNotification notification = new TaskNotification();
            notification.setTaskId(taskId);
            notification.setStatus("FAILED");
            notification.setErrorMessage(task.getErrorMessage());
            sendToSession(session, notification);
            log.info("subscribe 时推送已失败任务: taskId={}", taskId);
        }
    }

    private void sendToSession(WebSocketSession session, TaskNotification notification) {
        try {
            String json = objectMapper.writeValueAsString(notification);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("推送消息失败: sessionId={}", session.getId(), e);
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if ("token".equals(pair[0]) && pair.length == 2) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    @Data
    public static class SubscribeMessage {
        private String action;
        private Long taskId;
    }

    @Data
    public static class TaskNotification {
        private Long taskId;
        private String status;
        private String imageUrl;
        private String revisedPrompt;
        private String errorMessage;
    }
}
