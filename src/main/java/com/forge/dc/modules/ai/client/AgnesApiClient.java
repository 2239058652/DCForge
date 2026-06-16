package com.forge.dc.modules.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AgnesApiClient {

    @Value("${agnes.api-key}")
    private String apiKey;

    @Value("${agnes.base-url}")
    private String baseUrl;

    @Value("${agnes.model}")
    private String model;

    @Value("${agnes.timeout}")
    private int timeout;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile OkHttpClient httpClient;

    private OkHttpClient getClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(timeout, TimeUnit.SECONDS)
                            .readTimeout(timeout, TimeUnit.SECONDS)
                            .writeTimeout(timeout, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return httpClient;
    }

    public AgnesImageResponse generateImage(String prompt, String size, boolean returnUrl) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("size", size);

            if (returnUrl) {
                ObjectNode extraBody = objectMapper.createObjectNode();
                extraBody.put("response_format", "url");
                body.set("extra_body", extraBody);
            } else {
                body.put("return_base64", true);
            }

            return doPost(body);
        } catch (Exception e) {
            log.error("Agnes AI 文生图调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("图像生成失败: " + e.getMessage(), e);
        }
    }

    public AgnesImageResponse imageToImage(String prompt, String size, List<String> images, boolean returnUrl) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("size", size);

            ObjectNode extraBody = objectMapper.createObjectNode();
            ArrayNode imageArray = extraBody.putArray("image");
            images.forEach(imageArray::add);

            if (returnUrl) {
                extraBody.put("response_format", "url");
            } else {
                extraBody.put("response_format", "b64_json");
            }

            body.set("extra_body", extraBody);

            return doPost(body);
        } catch (Exception e) {
            log.error("Agnes AI 图生图调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("图像生成失败: " + e.getMessage(), e);
        }
    }

    private AgnesImageResponse doPost(ObjectNode body) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("Agnes AI 请求: {}", jsonBody);

        Request request = new Request.Builder()
                .url(baseUrl + "/v1/images/generations")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("Agnes AI 响应状态: {}, body: {}", response.code(), responseBody);

            if (!response.isSuccessful()) {
                throw new RuntimeException("Agnes AI 返回错误状态 " + response.code() + ": " + responseBody);
            }

            return objectMapper.readValue(responseBody, AgnesImageResponse.class);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AgnesImageResponse {
        private Long created;
        private List<ImageData> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageData {
        private String url;

        @JsonProperty("b64_json")
        private String b64Json;

        @JsonProperty("revised_prompt")
        private String revisedPrompt;
    }
}
