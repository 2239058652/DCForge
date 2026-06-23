package com.forge.dc.modules.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.forge.dc.modules.ai.dto.ChatCompletionRequestDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionResponseDTO;
import com.forge.dc.modules.ai.dto.ChatMessageDTO;
import com.forge.dc.modules.ai.dto.StreamChatRequestDTO;
import com.forge.dc.modules.ai.service.ChatCompletionService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatCompletionServiceImpl implements ChatCompletionService {

    @Value("${agnes.chat.api-key:}")
    private String chatApiKey;

    @Value("${agnes.chat.base-url:https://apihub.agnes-ai.com}")
    private String chatBaseUrl;

    @Value("${agnes.chat.model:agnes-2.0-flash}")
    private String chatModel;

    @Value("${agnes.chat.timeout:120}")
    private int chatTimeout;

    private final ObjectMapper objectMapper;

    public ChatCompletionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    private volatile OkHttpClient httpClient;

    private OkHttpClient getClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(chatTimeout, TimeUnit.SECONDS)
                            .readTimeout(chatTimeout, TimeUnit.SECONDS)
                            .writeTimeout(chatTimeout, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return httpClient;
    }

    @Override
    public ChatCompletionResponseDTO chatCompletion(ChatCompletionRequestDTO request) throws IOException {
        ObjectNode body = buildRequestBody(request, false);

        JsonNode messagesNode = body.path("messages");
        if (!messagesNode.isArray() || messagesNode.size() == 0) {
            throw new IllegalArgumentException("消息列表不能为空");
        }

        String jsonBody = objectMapper.writeValueAsString(body);

        log.info("Agnes Chat 请求: model={}, messages={}", chatModel, messagesNode.size());

        Request httpRequest = new Request.Builder()
                .url(chatBaseUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + chatApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = getClient().newCall(httpRequest).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("Agnes Chat 响应状态: {}", response.code());

            if (response.code() >= 500 || response.code() == 429) {
                throw new IOException("Agnes Chat 服务端错误: HTTP " + response.code());
            }
            if (response.code() != 200) {
                throw new IllegalStateException("Agnes Chat 请求错误 " + response.code() + ": " + responseBody);
            }

            return objectMapper.readValue(responseBody, ChatCompletionResponseDTO.class);
        }
    }

    @Override
    public Call streamChatCompletion(StreamChatRequestDTO request, StreamCallback callback) throws IOException {
        ObjectNode body = buildStreamRequestBody(request);
        String jsonBody = objectMapper.writeValueAsString(body);

        log.info("Agnes Stream Chat 请求: model={}, sessionId={}", chatModel, request.getSessionId());

        Request httpRequest = new Request.Builder()
                .url(chatBaseUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + chatApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        Call call = getClient().newCall(httpRequest);
        StringBuilder fullContent = new StringBuilder();

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    log.info("Agnes Stream Chat 已取消: sessionId={}", request.getSessionId());
                    return;
                }
                log.error("Agnes Stream Chat 异常: sessionId={}", request.getSessionId(), e);
                callback.onError("流式请求异常: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) {
                    log.info("Agnes Stream Chat 已取消: sessionId={}", request.getSessionId());
                    response.close();
                    return;
                }

                if (response.code() != 200) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Agnes Stream Chat 请求错误 {}: {}", response.code(), errorBody);
                    callback.onError("请求失败: HTTP " + response.code());
                    return;
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    callback.onError("响应体为空");
                    return;
                }

                try (InputStream inputStream = responseBody.byteStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (call.isCanceled()) {
                            log.info("Agnes Stream Chat 已取消: sessionId={}", request.getSessionId());
                            return;
                        }

                        if (line.isEmpty()) {
                            continue;
                        }

                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);

                            if ("[DONE]".equals(data)) {
                                log.info("Agnes Stream Chat 完成: sessionId={}", request.getSessionId());
                                callback.onComplete(fullContent.toString());
                                return;
                            }

                            try {
                                JsonNode chunk = objectMapper.readTree(data);
                                JsonNode choices = chunk.path("choices");
                                if (choices.isArray() && !choices.isEmpty()) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    String content = delta.path("content").asText();
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        callback.onMessage(content);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("解析流式响应失败: {}", data, e);
                            }
                        }
                    }

                    if (call.isCanceled()) {
                        log.info("Agnes Stream Chat 已取消: sessionId={}", request.getSessionId());
                        return;
                    }

                    if (fullContent.length() > 0) {
                        log.info("Agnes Stream Chat 流结束但未收到 [DONE]: sessionId={}", request.getSessionId());
                        callback.onComplete(fullContent.toString());
                    } else {
                        callback.onError("流异常结束，未收到任何内容");
                    }
                } catch (IOException e) {
                    if (call.isCanceled()) {
                        log.info("Agnes Stream Chat 已取消: sessionId={}", request.getSessionId());
                        return;
                    }
                    log.error("Agnes Stream Chat 读取响应异常: sessionId={}", request.getSessionId(), e);
                    callback.onError("流式请求异常: " + e.getMessage());
                }
            }
        });

        return call;
    }

    private ObjectNode buildRequestBody(ChatCompletionRequestDTO request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", chatModel);

        ArrayNode messagesArray = body.putArray("messages");

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messagesArray.add(systemMsg);
        }

        for (ChatMessageDTO msg : request.getMessages()) {
            if (msg == null || !msg.isValid()) {
                continue;
            }
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            if (msg.getToolCallId() != null && !msg.getToolCallId().isBlank()) {
                msgNode.put("tool_call_id", msg.getToolCallId());
            }

            if (msg.getContent() instanceof String) {
                msgNode.put("content", (String) msg.getContent());
            } else if (msg.getContent() == null) {
                if (msg.getToolCalls() != null && !msg.getToolCalls().isNull()) {
                    msgNode.putNull("content");
                } else {
                    msgNode.put("content", "");
                }
            } else {
                msgNode.set("content", objectMapper.valueToTree(msg.getContent()));
            }
            if (msg.getToolCalls() != null && !msg.getToolCalls().isNull()) {
                msgNode.set("tool_calls", msg.getToolCalls());
            }

            messagesArray.add(msgNode);
        }

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTools() != null && !request.getTools().isNull()) {
            body.set("tools", request.getTools());
        }
        if (request.getToolChoice() != null && !request.getToolChoice().isNull()) {
            body.set("tool_choice", request.getToolChoice());
        }
        if (request.getStop() != null && !request.getStop().isNull()) {
            body.set("stop", request.getStop());
        }
        if (request.getChatTemplateKwargs() != null && !request.getChatTemplateKwargs().isNull()) {
            body.set("chat_template_kwargs", request.getChatTemplateKwargs());
        }
        body.put("stream", stream);

        return body;
    }

    private ObjectNode buildStreamRequestBody(StreamChatRequestDTO request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", chatModel);
        body.put("stream", true);

        ArrayNode messagesArray = body.putArray("messages");

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messagesArray.add(systemMsg);
        }

        for (ChatMessageDTO msg : request.getMessages()) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            if (msg.getToolCallId() != null && !msg.getToolCallId().isBlank()) {
                msgNode.put("tool_call_id", msg.getToolCallId());
            }

            if (msg.getContent() instanceof String) {
                msgNode.put("content", (String) msg.getContent());
            } else if (msg.getContent() == null) {
                if (msg.getToolCalls() != null && !msg.getToolCalls().isNull()) {
                    msgNode.putNull("content");
                } else {
                    msgNode.put("content", "");
                }
            } else {
                msgNode.set("content", objectMapper.valueToTree(msg.getContent()));
            }
            if (msg.getToolCalls() != null && !msg.getToolCalls().isNull()) {
                msgNode.set("tool_calls", msg.getToolCalls());
            }

            messagesArray.add(msgNode);
        }

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }

        return body;
    }
}
