# AgnesProxyController 代码审查与修复方案

## 问题汇总

| # | 严重程度 | 问题 | 文件 / 行 |
|---|----------|------|-----------|
| 1 | P0 🔴 | 裸线程泄漏 | `AgnesProxyController.java:156` |
| 2 | P0 🔴 | `readTimeout(0)` 无超时 + 无法取消 | `AgnesProxyController.java:87` |
| 3 | P1 🟠 | SseEmitter 断连后 OkHttp 调用不取消 | `AgnesProxyController.java:149` |
| 4 | P1 🟠 | `emitter.send()` 异常后重复 complete | `AgnesProxyController.java:218-222` |
| 5 | P2 🟡 | `buildOpenAIRequestBody` 与 ServiceImpl 重复 | `AgnesProxyController.java:273` |
| 6 | P2 🟡 | `@Value` 配置在 Controller 中重复声明 | `AgnesProxyController.java:58-68` |
| 7 | P2 🟡 | 三处各自 `new ObjectMapper()` | Controller / Service / ServiceImpl |
| 8 | P3 🔵 | 死代码 `convertOpenAIStreamChunkToAnthropic(String)` | `AgnesProxyService.java:158` |
| 9 | P3 🔵 | 重复日志（全量 JSON + 字段各打一次） | `AgnesProxyController.java:107-115` |
| 10 | P3 🔵 | 流式 ID 使用 `System.currentTimeMillis()` 存在碰撞 | `AgnesProxyService.java:225` |

---

## 问题详情与修复

---

### 问题 1 — P0: 裸线程泄漏

**位置**: `AgnesProxyController.java:156–241`

**原因**: 每个流式请求 `new Thread(...).start()`，无上限、无监控、无名称，高并发下线程数不受控制。

**修复**: 注入 Spring 管理的 `ExecutorService`。

```java
// AgnesProxyConfig.java（新增 @Configuration 类）
@Configuration
public class AgnesProxyConfig {

    @Bean(name = "agnesStreamExecutor", destroyMethod = "shutdown")
    public ExecutorService agnesStreamExecutor() {
        return new ThreadPoolExecutor(
                4, 50,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r, "agnes-stream-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

```java
// AgnesProxyController.java
@Qualifier("agnesStreamExecutor")
private final ExecutorService streamExecutor;

// handleStreamingResponse 中替换 new Thread(...).start()
streamExecutor.submit(() -> {
    // ... 原有逻辑
});
```

---

### 问题 2 — P0: `readTimeout(0)` 无超时

**位置**: `AgnesProxyController.java:87`

**原因**: `readTimeout(0)` 等价于永不超时。若 Agnes 服务端卡死，线程将永久阻塞，并随问题 1 叠加成不可恢复的线程泄漏。

**修复**:

```java
// 修改前
.readTimeout(0, TimeUnit.SECONDS)

// 修改后
.readTimeout(proxyStreamTimeout, TimeUnit.SECONDS)
```

`proxyStreamTimeout` 已有配置（默认 1800s），与 SseEmitter 超时对齐即可。

---

### 问题 3 — P1: SseEmitter 断连后 OkHttp 调用不取消

**位置**: `AgnesProxyController.java:149`

**原因**: SseEmitter 提供 `onCompletion / onTimeout / onError` 回调，但当前代码未注册任何回调。客户端断连或 Emitter 超时后，OkHttp 调用和线程仍在继续消耗资源。

**修复**: 在提交线程前构建 `Call` 对象并注册回调：

```java
private SseEmitter handleStreamingResponse(ChatCompletionRequestDTO openAIRequest) {
    openAIRequest.setStream(true);

    // 提前构建请求体，以便在线程外获取 Call 引用
    ObjectNode body;
    String jsonBody;
    try {
        body = buildOpenAIRequestBody(openAIRequest);
        jsonBody = objectMapper.writeValueAsString(body);
    } catch (Exception e) {
        log.error("构建 Agnes 请求体失败", e);
        SseEmitter error = new SseEmitter();
        error.completeWithError(e);
        return error;
    }

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

    String requestSummary = summarizeOpenAIRequestBody(body);
    log.info("转发 Agnes 流式请求: {}", requestSummary);

    streamExecutor.submit(() -> {
        AgnesProxyService.AnthropicStreamState streamState = new AgnesProxyService.AnthropicStreamState();
        try {
            String startEvent = agnesProxyService.buildAnthropicStreamStart(chatModel);
            if (startEvent != null) {
                emitter.send(SseEmitter.event().name("message_start").data(startEvent));
            }

            try (Response response = call.execute()) {   // 使用已有 Call，不再新建
                if (response.code() != 200) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Anthropic 流式请求错误 {}: {}, request={}", response.code(), errorBody, requestSummary);
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

    return emitter;
}
```

关键变化：
- `Call` 在 `streamExecutor.submit()` 之前创建，可在回调中引用
- `emitter.onCompletion/onTimeout/onError` 均调用 `call.cancel()`
- 异常处理中优先检查 `call.isCanceled()`，避免将正常取消当错误日志打印

---

### 问题 4 — P1: `emitter.send()` 异常后重复 complete

**位置**: `AgnesProxyController.java:218-222` / `sendAnthropicEvent`

**原因**: `emitter.send()` 在客户端已断连时会抛出 `IOException`，被外层 catch 捕获后再调 `emitter.completeWithError()`，而此时 emitter 可能已由 `onCompletion` 回调置为完成，导致无意义的异常日志。

**修复**（在问题 3 的修复基础上）:

```java
private void sendAnthropicEvent(SseEmitter emitter,
                                AgnesProxyService.AnthropicStreamEvent event) throws IOException {
    if (event == null || event.data() == null || event.data().isEmpty()) {
        return;
    }
    emitter.send(SseEmitter.event().name(event.event()).data(event.data()));
}
```

外层统一 catch 处理（已在问题 3 修复中体现）：
```java
} catch (Exception e) {
    if (call.isCanceled()) {
        log.info("Agnes 流式请求已取消（客户端断连）");
        return;                     // 不打 error，不再 complete
    }
    log.error("Anthropic 流式处理异常", e);
    try { emitter.completeWithError(e); } catch (Exception ignored) {}
}
```

---

### 问题 5 — P2: `buildOpenAIRequestBody` 与 `ChatCompletionServiceImpl` 重复

**位置**: `AgnesProxyController.java:273` vs `ChatCompletionServiceImpl.buildRequestBody()`

**原因**: 两处构建 OpenAI 请求体的逻辑几乎完全一致，维护时需要同步修改两处，容易遗漏。

**修复**: 将构建逻辑下沉到 `AgnesProxyService`，复用现有的 `convertAnthropicToOpenAI` + 复用 ServiceImpl 的 `chatCompletion`。

流式路径直接复用 `ChatCompletionService` 接口（如果该接口提供流式方法）；若流式请求必须走 Controller 内的 OkHttp，则把 body 构建提取为 `AgnesProxyService.buildOpenAIBody(ChatCompletionRequestDTO, boolean stream)` 单一方法，Controller 调用。

---

### 问题 6 — P2: `@Value` 配置在 Controller 中重复声明

**位置**: `AgnesProxyController.java:58-68`

`chatApiKey`、`chatBaseUrl`、`chatModel`、`chatTimeout` 已在 `ChatCompletionServiceImpl` 中声明。Controller 重复读取 `${agnes.chat.*}` 并自行维护 OkHttp 实例，导致：
- 任何配置扩展（如连接池、拦截器）需两处修改
- 逻辑职责不清晰

**修复**: 将流式 HTTP 调用移入 `ChatCompletionService`（增加流式方法），或将共享 OkHttp 实例和配置提取为独立 Bean，Controller 不持有 HTTP 客户端。

---

### 问题 7 — P2: 三处各自 `new ObjectMapper()`

`AgnesProxyController`、`AgnesProxyService`、`ChatCompletionServiceImpl` 各创建一个 `ObjectMapper`。

**修复**: 注入共享 Bean：

```java
// AgnesProxyConfig.java 中
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}

// 各类中替换
private final ObjectMapper objectMapper;  // 构造注入
```

---

### 问题 8 — P3: 死代码

**位置**: `AgnesProxyService.java:158-165`

`convertOpenAIStreamChunkToAnthropic(String)` 创建了独立的 `AnthropicStreamState`，不持有跨 chunk 的状态，实际流式逻辑无法正确使用。Controller 中也未调用此方法（使用的是带 `state` 参数的重载）。

**修复**: 直接删除该方法。

---

### 问题 9 — P3: 重复日志

**位置**: `AgnesProxyController.java:107-115`

第 107-111 行将整个请求序列化为 JSON 字符串（含 500 字截断）再打印，第 112-115 行再打一次字段级日志。JSON 序列化有额外开销。

**修复**: 删除 107-111 行，只保留字段级日志。

---

### 问题 10 — P3: 流式消息 ID 碰撞风险

**位置**: `AgnesProxyService.java:225`

```java
message.put("id", "msg_proxy_" + System.currentTimeMillis());
```

毫秒级精度在高并发下会重复。Claude Code 不校验 ID 唯一性，实际影响较小，但仍建议修复。

**修复**:

```java
// 使用 UUID 或 AtomicLong
private static final AtomicLong MSG_COUNTER = new AtomicLong();

message.put("id", "msg_proxy_" + MSG_COUNTER.incrementAndGet());
```

---

## 修复优先级建议

```
立即修复（P0/P1）:
  问题 1 → 问题 2 → 问题 3（三者关联，建议一起修）→ 问题 4

计划修复（P2，下一个迭代）:
  问题 5 + 问题 6（配置与逻辑统一归属）
  问题 7（注入共享 ObjectMapper）

随手修复（P3，低风险）:
  问题 8 删死代码
  问题 9 删重复日志
  问题 10 换 AtomicLong
```

---

## 其他说明

**背压（被提及的问题 4）**  
实际影响有限。OkHttp 从 Agnes 读取的速度由 TCP 接收窗口决定，`emitter.send()` 底层写 Servlet OutputStream，若客户端 TCP 窗口满会自动阻塞整个读取线程，不会 OOM。在修复问题 1（线程池有界）后，最坏情况是线程阻塞在 `emitter.send()`，不会无限堆积内存。无需额外实现背压逻辑。

**`/v1/messages` 无鉴权**  
当前端点无任何认证。若此代理与 Claude Code 运行在同一受信内网，可接受；若暴露在公网必须加认证（如固定 API Key Header 校验或 IP 白名单）。
