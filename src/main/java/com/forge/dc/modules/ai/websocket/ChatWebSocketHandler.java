package com.forge.dc.modules.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.common.util.JwtUtils;
import com.forge.dc.modules.ai.dto.ChatMessageDTO;
import com.forge.dc.modules.ai.dto.StreamChatRequestDTO;
import com.forge.dc.modules.ai.service.ChatCompletionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtils jwtUtils;
    private final ChatCompletionService chatCompletionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessageDTO>> sessionMessages = new ConcurrentHashMap<>();
    private final Map<String, okhttp3.Call> sessionCalls = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionProcessing = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(JwtUtils jwtUtils, ChatCompletionService chatCompletionService) {
        this.jwtUtils = jwtUtils;
        this.chatCompletionService = chatCompletionService;
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
        sessions.put(session.getId(), session);
        sessionMessages.put(session.getId(), new ArrayList<>());

        log.info("Chat WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        log.info("收到 Chat WebSocket 消息: sessionId={}", sessionId);

        boolean acquiredProcessing = false;
        try {
            ChatRequestMessage requestMsg = objectMapper.readValue(payload, ChatRequestMessage.class);

            if ("message".equals(requestMsg.getAction())) {
                Boolean previous = sessionProcessing.putIfAbsent(sessionId, true);
                if (Boolean.TRUE.equals(previous)) {
                    sendError(session, "正在生成中，请稍后再试");
                    return;
                }
                acquiredProcessing = true;
                handleChatMessage(session, requestMsg);
            } else if ("stop".equals(requestMsg.getAction())) {
                handleStop(session);
            } else if ("clear".equals(requestMsg.getAction())) {
                handleClear(session);
            }
        } catch (Exception e) {
            log.error("处理 Chat WebSocket 消息失败", e);
            sendError(session, "消息处理失败: " + e.getMessage());
            if (acquiredProcessing) {
                sessionProcessing.remove(sessionId);
            }
        }
    }

    private void handleChatMessage(WebSocketSession session, ChatRequestMessage requestMsg) throws IOException {
        String sessionId = session.getId();
        List<ChatMessageDTO> messages = sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());

        String systemPrompt = requestMsg.getSystemPrompt();

        if (requestMsg.getContent() != null && !requestMsg.getContent().isBlank()) {
            ChatMessageDTO userMsg = new ChatMessageDTO();
            userMsg.setRole("user");
            userMsg.setContent(requestMsg.getContent());
            messages.add(userMsg);
        }

        List<ChatMessageDTO> messagesCopy = new ArrayList<>(messages);
        messagesCopy.removeIf(msg -> msg == null || !msg.isValid());

        if (messagesCopy.isEmpty()) {
            sendError(session, "没有有效的消息");
            sessionProcessing.remove(sessionId);
            return;
        }

        StreamChatRequestDTO request = new StreamChatRequestDTO();
        request.setSessionId(sessionId);
        request.setMessages(messagesCopy);
        request.setSystemPrompt(systemPrompt);
        request.setTemperature(requestMsg.getTemperature());
        request.setMaxTokens(requestMsg.getMaxTokens() != null ? requestMsg.getMaxTokens() : 2048);

        sendStatus(session, "processing");

        AtomicBoolean terminal = new AtomicBoolean(false);
        okhttp3.Call call = chatCompletionService.streamChatCompletion(request, new ChatCompletionService.StreamCallback() {
            private final StringBuilder fullContent = new StringBuilder();

            @Override
            public void onMessage(String content) {
                fullContent.append(content);
                try {
                    sendChunk(session, content);
                } catch (IOException e) {
                    log.error("发送流式消息失败", e);
                }
            }

            @Override
            public void onComplete(String content) {
                if (!terminal.compareAndSet(false, true)) return;

                ChatMessageDTO assistantMsg = new ChatMessageDTO();
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(content);
                messages.add(assistantMsg);

                try {
                    sendDone(session, content);
                } catch (IOException e) {
                    log.error("发送完成消息失败", e);
                }
                sessionCalls.remove(sessionId);
                sessionProcessing.remove(sessionId);
            }

            @Override
            public void onError(String errorMessage) {
                if (!terminal.compareAndSet(false, true)) return;

                try {
                    sendError(session, errorMessage);
                } catch (IOException e) {
                    log.error("发送错误消息失败", e);
                }
                sessionCalls.remove(sessionId);
                sessionProcessing.remove(sessionId);
            }
        });
        sessionCalls.put(sessionId, call);
        if (terminal.get()) {
            sessionCalls.remove(sessionId, call);
        }
    }

    private void handleStop(WebSocketSession session) {
        String sessionId = session.getId();
        okhttp3.Call call = sessionCalls.remove(sessionId);
        if (call != null && !call.isCanceled()) {
            call.cancel();
            log.info("用户停止生成: sessionId={}", sessionId);
        }
        sessionProcessing.remove(sessionId);

        try {
            sendStatus(session, "stopped");
        } catch (IOException e) {
            log.error("发送停止状态失败", e);
        }
    }

    private void handleClear(WebSocketSession session) {
        String sessionId = session.getId();
        sessionMessages.put(sessionId, new ArrayList<>());
        log.info("对话历史已清除: sessionId={}", sessionId);

        try {
            sendStatus(session, "cleared");
        } catch (IOException e) {
            log.error("发送清除状态失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        okhttp3.Call call = sessionCalls.remove(sessionId);
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
        sessions.remove(sessionId);
        sessionMessages.remove(sessionId);
        sessionProcessing.remove(sessionId);

        log.info("Chat WebSocket 连接关闭: sessionId={}, status={}", sessionId, status);
    }

    private void sendChunk(WebSocketSession session, String content) throws IOException {
        WebSocketMessage<String> message = new TextMessage(
                objectMapper.writeValueAsString(Map.of(
                        "type", "chunk",
                        "content", content
                ))
        );
        session.sendMessage(message);
    }

    private void sendDone(WebSocketSession session, String fullContent) throws IOException {
        WebSocketMessage<String> message = new TextMessage(
                objectMapper.writeValueAsString(Map.of(
                        "type", "done",
                        "content", fullContent
                ))
        );
        session.sendMessage(message);
    }

    private void sendStatus(WebSocketSession session, String status) throws IOException {
        WebSocketMessage<String> message = new TextMessage(
                objectMapper.writeValueAsString(Map.of(
                        "type", "status",
                        "status", status
                ))
        );
        session.sendMessage(message);
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        WebSocketMessage<String> message = new TextMessage(
                objectMapper.writeValueAsString(Map.of(
                        "type", "error",
                        "message", errorMessage
                ))
        );
        session.sendMessage(message);
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
    public static class ChatRequestMessage {
        private String action;
        private String content;
        private String systemPrompt;
        private Double temperature;
        private Integer maxTokens;
    }
}
