package com.forge.dc.modules.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.forge.dc.modules.ai.dto.AnthropicRequestDTO;
import com.forge.dc.modules.ai.dto.AnthropicResponseDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionRequestDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionResponseDTO;
import com.forge.dc.modules.ai.service.AgnesProxyService;
import com.forge.dc.modules.ai.service.ChatCompletionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Agnes 代理控制器
 * 提供 Anthropic 兼容的 API 端点，将请求转换后转发到 Agnes (OpenAI 格式)
 *
 * 端点:
 * - POST /v1/messages - 同步/流式聊天（由请求体 stream 字段决定）
 */
@Slf4j
@RestController
public class AgnesProxyController {

    private final AgnesProxyService agnesProxyService;
    private final ChatCompletionService chatCompletionService;
    private final ObjectMapper objectMapper;
    private final ExecutorService streamExecutor;

    @Value("${agnes.chat.api-key:}")
    private String chatApiKey;

    @Value("${agnes.chat.base-url:https://apihub.agnes-ai.com}")
    private String chatBaseUrl;

    @Value("${agnes.chat.model:agnes-2.0-flash}")
    private String chatModel;

    @Value("${agnes.proxy.stream-timeout:1800}")
    private long proxyStreamTimeout;

    private volatile OkHttpClient httpClient;

    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(proxyStreamTimeout, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return httpClient;
    }

    public AgnesProxyController(AgnesProxyService agnesProxyService,
                                ChatCompletionService chatCompletionService,
                                ObjectMapper objectMapper,
                                @Qualifier("agnesStreamExecutor") ExecutorService streamExecutor) {
        this.agnesProxyService = agnesProxyService;
        this.chatCompletionService = chatCompletionService;
        this.objectMapper = objectMapper;
        this.streamExecutor = streamExecutor;
    }

    /**
     * Anthropic 兼容的消息端点
     * 接收 Anthropic 格式请求，返回 Anthropic 格式响应
     */
    @PostMapping(value = "/v1/messages",
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object messages(@RequestBody AnthropicRequestDTO anthropicRequest,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        log.info("收到 Anthropic 消息请求: model={}, stream={}, messages={}",
                anthropicRequest.getModel(),
                anthropicRequest.getStream(),
                anthropicRequest.getMessages() != null ? anthropicRequest.getMessages().size() : 0);

        // 转换 Anthropic -> OpenAI 格式
        ChatCompletionRequestDTO openAIRequest = agnesProxyService.convertAnthropicToOpenAI(anthropicRequest);

        if (Boolean.TRUE.equals(anthropicRequest.getStream())) {
            return handleStreamingResponse(openAIRequest);
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            return handleSyncResponse(openAIRequest);
        }
    }

    /**
     * 处理同步响应
     */
    private ResponseEntity<AnthropicResponseDTO> handleSyncResponse(ChatCompletionRequestDTO openAIRequest) {
        try {
            ChatCompletionResponseDTO openAIResponse = chatCompletionService.chatCompletion(openAIRequest);
            AnthropicResponseDTO anthropicResponse = agnesProxyService.convertOpenAIToAnthropic(openAIResponse);
            return ResponseEntity.ok(anthropicResponse);
        } catch (Exception e) {
            log.error("Anthropic 同步消息处理失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("api_error", e.getMessage()));
        }
    }

    /**
     * 流式响应处理器
     */
    private SseEmitter handleStreamingResponse(ChatCompletionRequestDTO openAIRequest) {
        openAIRequest.setStream(true);

        // 提前构建请求体，以便在线程外获取 Call 引用
        ObjectNode body;
        String jsonBody;
        try {
            body = agnesProxyService.buildOpenAIRequestBody(openAIRequest, chatModel, true);
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("构建 Agnes 请求体失败", e);
            SseEmitter error = new SseEmitter();
            error.completeWithError(e);
            return error;
        }

        String requestSummary = agnesProxyService.summarizeOpenAIRequestBody(body);
        log.info("转发 Agnes 流式请求: {}", requestSummary);

        Request httpRequest = new Request.Builder()
                .url(chatBaseUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + chatApiKey)
                .addHeader("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                .build();

        Call call = getHttpClient().newCall(httpRequest);
        SseEmitter emitter = new SseEmitter(TimeUnit.SECONDS.toMillis(proxyStreamTimeout));

        // 注册断连/超时回调，确保 Agnes 请求被取消
        emitter.onCompletion(call::cancel);
        emitter.onTimeout(call::cancel);
        emitter.onError(t -> call.cancel());

        try {
            streamExecutor.submit(() -> {
            AgnesProxyService.AnthropicStreamState streamState = new AgnesProxyService.AnthropicStreamState();
            try {
                // 发送流式开始事件
                String startEvent = agnesProxyService.buildAnthropicStreamStart(chatModel);
                if (startEvent != null) {
                    emitter.send(SseEmitter.event().name("message_start").data(startEvent));
                }

                try (Response response = call.execute()) {
                    if (response.code() != 200) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        log.error("Anthropic 流式请求错误 {}: {}, request={}",
                                response.code(), errorBody, requestSummary);
                        emitter.send(SseEmitter.event().name("error")
                                .data(createStreamErrorEvent("api_error", "HTTP " + response.code())));
                        emitter.complete();
                        return;
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        emitter.complete();
                        return;
                    }

                    try (InputStream inputStream = responseBody.byteStream();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) continue;
                            if (!line.startsWith("data: ")) continue;

                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                for (AgnesProxyService.AnthropicStreamEvent event
                                        : agnesProxyService.buildAnthropicStreamEndEvents(streamState)) {
                                    sendAnthropicEvent(emitter, event);
                                }
                                emitter.complete();
                                return;
                            }

                            for (AgnesProxyService.AnthropicStreamEvent event
                                    : agnesProxyService.convertOpenAIStreamChunkToAnthropicEvents(data, streamState)) {
                                sendAnthropicEvent(emitter, event);
                            }
                        }
                    }

                    // 流正常结束但未收到 [DONE]
                    for (AgnesProxyService.AnthropicStreamEvent event
                            : agnesProxyService.buildAnthropicStreamEndEvents(streamState)) {
                        sendAnthropicEvent(emitter, event);
                    }
                    emitter.complete();
                }

            } catch (Exception e) {
                if (call.isCanceled()) {
                    log.info("Agnes 流式请求已取消（客户端断连）");
                    return;
                }
                log.error("Anthropic 流式处理异常", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // emitter 已关闭，忽略
                }
            }
        });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            log.error("流式请求被拒绝（线程池已满）", e);
            call.cancel();
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data(createStreamErrorEvent("overloaded", "Server is busy, please try again later")));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(e);
            }
        }

        return emitter;
    }

    private void sendAnthropicEvent(SseEmitter emitter,
                                    AgnesProxyService.AnthropicStreamEvent event) throws IOException {
        if (event == null || event.data() == null || event.data().isEmpty()) {
            return;
        }
        emitter.send(SseEmitter.event().name(event.event()).data(event.data()));
    }

    /**
     * Anthropic 兼容的模型信息端点
     */
    @GetMapping(value = "/v1/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> models() {
        ObjectNode response = objectMapper.createObjectNode();
        response.putArray("data").addObject()
                .put("type", "model")
                .put("id", chatModel)
                .put("display_name", "Agnes 2.0 Flash (Proxy)");
        response.put("has_more", false);
        response.put("first_id", chatModel);
        response.put("last_id", chatModel);

        return ResponseEntity.ok(response.toString());
    }

    /**
     * 创建 Anthropic 格式的错误响应
     */
    private AnthropicResponseDTO createErrorResponse(String type, String message) {
        AnthropicResponseDTO error = new AnthropicResponseDTO();
        error.setType("error");

        AnthropicResponseDTO.ErrorDetail errorDetail = new AnthropicResponseDTO.ErrorDetail();
        errorDetail.setType(type);
        errorDetail.setMessage(message);
        error.setError(errorDetail);

        return error;
    }

    /**
     * 创建流式错误事件
     */
    private String createStreamErrorEvent(String type, String message) {
        try {
            ObjectNode errorEvent = objectMapper.createObjectNode();
            errorEvent.put("type", "error");

            ObjectNode error = objectMapper.createObjectNode();
            error.put("type", type);
            error.put("message", message);
            errorEvent.set("error", error);

            return objectMapper.writeValueAsString(errorEvent);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"error\":{\"type\":\"" + type + "\",\"message\":\"" + message + "\"}}";
        }
    }
}
