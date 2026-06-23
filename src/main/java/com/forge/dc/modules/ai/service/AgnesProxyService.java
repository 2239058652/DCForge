package com.forge.dc.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.forge.dc.modules.ai.dto.AnthropicRequestDTO;
import com.forge.dc.modules.ai.dto.AnthropicResponseDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionRequestDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionResponseDTO;
import com.forge.dc.modules.ai.dto.ChatMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agnes 代理服务
 * 负责 Anthropic API 格式与 OpenAI API 格式之间的转换
 */
@Slf4j
@Service
public class AgnesProxyService {

    private final ObjectMapper objectMapper;
    private static final AtomicLong MSG_COUNTER = new AtomicLong();

    public AgnesProxyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record AnthropicStreamEvent(String event, String data) {
    }

    public static class AnthropicStreamState {
        private int nextContentBlockIndex = 0;
        private int textBlockIndex = -1;
        private boolean textBlockStarted;
        private boolean textBlockStopped;
        private boolean messageDeltaSent;
        private final Map<Integer, ToolCallStreamState> toolCalls = new LinkedHashMap<>();
    }

    private static class ToolCallStreamState {
        private int contentBlockIndex = -1;
        private String id;
        private String name;
        private boolean started;
        private boolean stopped;
    }

    /**
     * 将 Anthropic 格式请求转换为 OpenAI 格式
     */
    public ChatCompletionRequestDTO convertAnthropicToOpenAI(AnthropicRequestDTO anthropicRequest) {
        ChatCompletionRequestDTO openAIRequest = new ChatCompletionRequestDTO();

        // 提取 system prompt 到顶层（system 可以是字符串或数组）
        String systemPrompt = extractTextFromContent(anthropicRequest.getSystem());
        if (!systemPrompt.isBlank()) {
            openAIRequest.setSystemPrompt(systemPrompt);
        }

        // 转换 messages（移除 system role，因为 Anthropic 不支持）
        List<ChatMessageDTO> messages = new ArrayList<>();
        if (anthropicRequest.getMessages() != null) {
            for (AnthropicRequestDTO.AnthropicMessage msg : anthropicRequest.getMessages()) {
                if (msg == null || msg.getRole() == null) {
                    continue;
                }
            if ("system".equals(msg.getRole())) {
                // 如果消息中有 system role，合并到 system prompt
                String newContent = extractTextFromContent(msg.getContent());
                    appendSystemPrompt(openAIRequest, newContent);
                continue;
            }

                messages.addAll(convertAnthropicMessageToOpenAI(msg));
            }
        }
        openAIRequest.setMessages(messages);

        // 直接映射的字段
        openAIRequest.setTemperature(anthropicRequest.getTemperature());
        openAIRequest.setTopP(anthropicRequest.getTopP());
        openAIRequest.setMaxTokens(anthropicRequest.getMaxTokens());
        openAIRequest.setStream(anthropicRequest.getStream());
        openAIRequest.setTools(convertAnthropicTools(anthropicRequest.getTools()));
        openAIRequest.setToolChoice(convertAnthropicToolChoice(anthropicRequest.getToolChoice()));
        if (anthropicRequest.getStopSequences() != null && !anthropicRequest.getStopSequences().isEmpty()) {
            openAIRequest.setStop(objectMapper.valueToTree(anthropicRequest.getStopSequences()));
        }
        openAIRequest.setChatTemplateKwargs(convertAnthropicThinking(anthropicRequest.getThinking()));

        return openAIRequest;
    }

    /**
     * 将 OpenAI 格式响应转换为 Anthropic 格式
     */
    public AnthropicResponseDTO convertOpenAIToAnthropic(ChatCompletionResponseDTO openAIResponse) {
        AnthropicResponseDTO anthropicResponse = new AnthropicResponseDTO();

        // 基础字段映射
        anthropicResponse.setId(openAIResponse.getId());
        anthropicResponse.setType("message");
        anthropicResponse.setModel(openAIResponse.getModel());

        // 转换 choices -> content[]
        if (openAIResponse.getChoices() != null && !openAIResponse.getChoices().isEmpty()) {
            ChatCompletionResponseDTO.Choice choice = openAIResponse.getChoices().get(0);
            ChatCompletionResponseDTO.Message message = choice.getMessage();
            anthropicResponse.setRole(message != null && message.getRole() != null ? message.getRole() : "assistant");

            List<AnthropicResponseDTO.Content> contents = new ArrayList<>();
            if (message != null && message.getContent() != null && !message.getContent().isEmpty()) {
                AnthropicResponseDTO.Content content = new AnthropicResponseDTO.Content();
                content.setType("text");
                content.setText(message.getContent());
                contents.add(content);
            }
            if (message != null && message.getToolCalls() != null && message.getToolCalls().isArray()) {
                for (JsonNode toolCall : message.getToolCalls()) {
                    AnthropicResponseDTO.Content content = new AnthropicResponseDTO.Content();
                    content.setType("tool_use");
                    content.setId(toolCall.path("id").asText());
                    JsonNode function = toolCall.path("function");
                    content.setName(function.path("name").asText());
                    content.setInput(parseToolArguments(function.path("arguments").asText()));
                    contents.add(content);
                }
            }
            if (contents.isEmpty()) {
                AnthropicResponseDTO.Content content = new AnthropicResponseDTO.Content();
                content.setType("text");
                content.setText("");
                contents.add(content);
            }
            anthropicResponse.setContent(contents);

            // finish_reason -> stop_reason
            anthropicResponse.setStopReason(mapStopReason(choice.getFinishReason()));
        }

        // 转换 usage
        if (openAIResponse.getUsage() != null) {
            AnthropicResponseDTO.Usage usage = new AnthropicResponseDTO.Usage();
            usage.setInputTokens(openAIResponse.getUsage().getPromptTokens());
            usage.setOutputTokens(openAIResponse.getUsage().getCompletionTokens());
            anthropicResponse.setUsage(usage);
        }

        return anthropicResponse;
    }

    /**
     * 构建 OpenAI 格式的请求体 JSON
     */
    public ObjectNode buildOpenAIRequestBody(ChatCompletionRequestDTO request, String model, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);

        ArrayNode messagesArray = body.putArray("messages");

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messagesArray.add(systemMsg);
        }

        if (request.getMessages() != null) {
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

    /**
     * 构建请求体摘要（用于日志，不打印完整内容）
     */
    public String summarizeOpenAIRequestBody(ObjectNode body) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("model", body.path("model").asText());
        summary.put("stream", body.path("stream").asBoolean(false));
        summary.put("messages", body.path("messages").isArray() ? body.path("messages").size() : 0);

        if (body.has("tools")) {
            summary.put("tools", body.path("tools").isArray() ? body.path("tools").size() : 1);
        }
        return summary.toString();
    }

    public List<AnthropicStreamEvent> convertOpenAIStreamChunkToAnthropicEvents(String openAIChunkData,
                                                                               AnthropicStreamState state) {
        List<AnthropicStreamEvent> events = new ArrayList<>();
        try {
            JsonNode chunk = objectMapper.readTree(openAIChunkData);

            JsonNode choices = chunk.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).path("delta");
                JsonNode content = delta.path("content");

                if (!content.isMissingNode() && !content.isNull() && !content.asText().isEmpty()) {
                    ensureTextBlockStarted(events, state);
                    events.add(buildTextDeltaEvent(state.textBlockIndex, content.asText()));
                }

                JsonNode toolCalls = delta.path("tool_calls");
                if (toolCalls.isArray()) {
                    for (JsonNode toolCallDelta : toolCalls) {
                        appendToolCallDelta(events, state, toolCallDelta);
                    }
                }

                JsonNode finishReason = choices.get(0).path("finish_reason");
                if (!finishReason.isMissingNode() && !finishReason.isNull() && !finishReason.asText().isEmpty()) {
                    stopOpenContentBlocks(events, state);
                    events.add(buildMessageDeltaEvent(mapStopReason(finishReason.asText()), chunk.path("usage")));
                    state.messageDeltaSent = true;
                }
            }

            return events;
        } catch (Exception e) {
            log.error("转换流式响应块失败: {}", openAIChunkData, e);
            return List.of();
        }
    }

    public List<AnthropicStreamEvent> buildAnthropicStreamEndEvents(AnthropicStreamState state) {
        List<AnthropicStreamEvent> events = new ArrayList<>();
        stopOpenContentBlocks(events, state);
        if (!state.messageDeltaSent) {
            events.add(buildMessageDeltaEvent("end_turn", null));
            state.messageDeltaSent = true;
        }
        events.add(new AnthropicStreamEvent("message_stop", buildAnthropicStreamEnd()));
        return events;
    }

    /**
     * 构建 Anthropic 格式的流式消息开始事件
     */
    public String buildAnthropicStreamStart(String model) {
        try {
            ObjectNode startEvent = objectMapper.createObjectNode();
            startEvent.put("type", "message_start");

            ObjectNode message = objectMapper.createObjectNode();
            message.put("id", "msg_proxy_" + MSG_COUNTER.incrementAndGet());
            message.put("type", "message");
            message.put("role", "assistant");
            message.put("model", model);
            message.putArray("content");
            startEvent.set("message", message);

            ObjectNode usage = objectMapper.createObjectNode();
            usage.put("input_tokens", 0);
            usage.put("output_tokens", 0);
            startEvent.set("usage", usage);

            return objectMapper.writeValueAsString(startEvent);
        } catch (Exception e) {
            log.error("构建流式开始事件失败", e);
            return null;
        }
    }

    /**
     * 构建 Anthropic 格式的流式消息结束事件
     */
    public String buildAnthropicStreamEnd() {
        try {
            ObjectNode endEvent = objectMapper.createObjectNode();
            endEvent.put("type", "message_stop");
            return objectMapper.writeValueAsString(endEvent);
        } catch (Exception e) {
            log.error("构建流式结束事件失败", e);
            return null;
        }
    }

    private void appendSystemPrompt(ChatCompletionRequestDTO request, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String existingPrompt = request.getSystemPrompt();
        request.setSystemPrompt(existingPrompt == null || existingPrompt.isBlank()
                ? content
                : existingPrompt + "\n" + content);
    }

    private List<ChatMessageDTO> convertAnthropicMessageToOpenAI(AnthropicRequestDTO.AnthropicMessage msg) {
        if ("assistant".equals(msg.getRole()) && msg.getContent() != null && msg.getContent().isArray()) {
            return List.of(convertAssistantContentBlocks(msg.getContent()));
        }
        if ("user".equals(msg.getRole()) && msg.getContent() != null && msg.getContent().isArray()) {
            return convertUserContentBlocks(msg.getContent());
        }

        ChatMessageDTO chatMsg = new ChatMessageDTO();
        chatMsg.setRole(msg.getRole());
        chatMsg.setContent(extractTextFromContent(msg.getContent()));
        return chatMsg.isValid() ? List.of(chatMsg) : List.of();
    }

    private ChatMessageDTO convertAssistantContentBlocks(JsonNode content) {
        ChatMessageDTO chatMsg = new ChatMessageDTO();
        chatMsg.setRole("assistant");

        StringBuilder text = new StringBuilder();
        ArrayNode toolCalls = objectMapper.createArrayNode();
        for (JsonNode block : content) {
            String type = block.path("type").asText();
            if ("text".equals(type)) {
                appendWithSpacing(text, block.path("text").asText());
            } else if ("tool_use".equals(type)) {
                ObjectNode toolCall = objectMapper.createObjectNode();
                toolCall.put("id", block.path("id").asText());
                toolCall.put("type", "function");

                ObjectNode function = objectMapper.createObjectNode();
                function.put("name", block.path("name").asText());
                function.put("arguments", block.path("input").isMissingNode()
                        ? "{}"
                        : block.path("input").toString());
                toolCall.set("function", function);
                toolCalls.add(toolCall);
            }
        }

        if (!toolCalls.isEmpty()) {
            chatMsg.setContent(text.isEmpty() ? null : text.toString());
            chatMsg.setToolCalls(toolCalls);
        } else {
            chatMsg.setContent(text.toString());
        }
        return chatMsg;
    }

    private List<ChatMessageDTO> convertUserContentBlocks(JsonNode content) {
        List<ChatMessageDTO> messages = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        for (JsonNode block : content) {
            String type = block.path("type").asText();
            if ("tool_result".equals(type)) {
                addUserTextMessage(messages, text);

                ChatMessageDTO toolMessage = new ChatMessageDTO();
                toolMessage.setRole("tool");
                toolMessage.setToolCallId(block.path("tool_use_id").asText());
                toolMessage.setContent(extractTextFromContent(block.path("content")));
                if (toolMessage.isValid()) {
                    messages.add(toolMessage);
                }
                continue;
            }

            if ("text".equals(type)) {
                appendWithSpacing(text, block.path("text").asText());
            } else {
                String extracted = extractTextFromContent(block);
                if (!extracted.isBlank()) {
                    appendWithSpacing(text, extracted);
                }
            }
        }

        addUserTextMessage(messages, text);
        return messages;
    }

    private void addUserTextMessage(List<ChatMessageDTO> messages, StringBuilder text) {
        if (text.isEmpty()) {
            return;
        }
        ChatMessageDTO userMessage = new ChatMessageDTO();
        userMessage.setRole("user");
        userMessage.setContent(text.toString());
        messages.add(userMessage);
        text.setLength(0);
    }

    private JsonNode convertAnthropicTools(JsonNode tools) {
        if (tools == null || tools.isNull() || !tools.isArray()) {
            return tools;
        }

        ArrayNode openAITools = objectMapper.createArrayNode();
        for (JsonNode tool : tools) {
            if ("function".equals(tool.path("type").asText()) && tool.has("function")) {
                openAITools.add(tool);
                continue;
            }
            String name = tool.path("name").asText();
            if (name.isBlank()) {
                continue;
            }

            ObjectNode openAITool = objectMapper.createObjectNode();
            openAITool.put("type", "function");

            ObjectNode function = objectMapper.createObjectNode();
            function.put("name", name);
            if (tool.hasNonNull("description")) {
                function.put("description", tool.path("description").asText());
            }
            function.set("parameters", tool.has("input_schema")
                    ? tool.path("input_schema")
                    : objectMapper.createObjectNode());
            openAITool.set("function", function);
            openAITools.add(openAITool);
        }
        return openAITools.isEmpty() ? null : openAITools;
    }

    private JsonNode convertAnthropicToolChoice(JsonNode toolChoice) {
        if (toolChoice == null || toolChoice.isNull()) {
            return null;
        }
        if (toolChoice.isTextual()) {
            return toolChoice;
        }
        String type = toolChoice.path("type").asText();
        return switch (type) {
            case "auto" -> objectMapper.valueToTree("auto");
            case "any" -> objectMapper.valueToTree("required");
            case "tool" -> {
                ObjectNode choice = objectMapper.createObjectNode();
                choice.put("type", "function");
                ObjectNode function = objectMapper.createObjectNode();
                function.put("name", toolChoice.path("name").asText());
                choice.set("function", function);
                yield choice;
            }
            default -> toolChoice;
        };
    }

    private JsonNode convertAnthropicThinking(JsonNode thinking) {
        if (thinking == null || thinking.isNull()) {
            return null;
        }
        if (!"enabled".equals(thinking.path("type").asText())) {
            return null;
        }
        ObjectNode chatTemplateKwargs = objectMapper.createObjectNode();
        chatTemplateKwargs.put("enable_thinking", true);
        return chatTemplateKwargs;
    }

    private JsonNode parseToolArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(arguments);
        } catch (Exception e) {
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("raw_arguments", arguments);
            return fallback;
        }
    }

    private String mapStopReason(String finishReason) {
        if (finishReason == null || finishReason.isBlank()) {
            return "end_turn";
        }
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls", "function_call" -> "tool_use";
            default -> finishReason;
        };
    }

    private void ensureTextBlockStarted(List<AnthropicStreamEvent> events, AnthropicStreamState state) {
        if (state.textBlockStarted) {
            return;
        }
        state.textBlockIndex = state.nextContentBlockIndex++;
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "content_block_start");
        event.put("index", state.textBlockIndex);

        ObjectNode contentBlock = objectMapper.createObjectNode();
        contentBlock.put("type", "text");
        contentBlock.put("text", "");
        event.set("content_block", contentBlock);

        state.textBlockStarted = true;
        events.add(new AnthropicStreamEvent("content_block_start", writeJson(event)));
    }

    private AnthropicStreamEvent buildTextDeltaEvent(int index, String text) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "content_block_delta");
        event.put("index", index);

        ObjectNode delta = objectMapper.createObjectNode();
        delta.put("type", "text_delta");
        delta.put("text", text);
        event.set("delta", delta);

        return new AnthropicStreamEvent("content_block_delta", writeJson(event));
    }

    private void appendToolCallDelta(List<AnthropicStreamEvent> events,
                                     AnthropicStreamState state,
                                     JsonNode toolCallDelta) {
        int openAIIndex = toolCallDelta.path("index").asInt(state.toolCalls.size());
        ToolCallStreamState toolState = state.toolCalls.computeIfAbsent(openAIIndex, ignored -> {
            ToolCallStreamState newState = new ToolCallStreamState();
            newState.contentBlockIndex = state.nextContentBlockIndex++;
            return newState;
        });

        if (toolCallDelta.hasNonNull("id")) {
            toolState.id = toolCallDelta.path("id").asText();
        }
        JsonNode function = toolCallDelta.path("function");
        if (function.hasNonNull("name") && !function.path("name").asText().isBlank()) {
            toolState.name = function.path("name").asText();
        }

        if (!toolState.started && toolState.name != null && !toolState.name.isBlank()) {
            events.add(buildToolUseStartEvent(toolState));
            toolState.started = true;
        }

        if (function.hasNonNull("arguments") && !function.path("arguments").asText().isEmpty()) {
            if (!toolState.started) {
                toolState.name = toolState.name == null ? "tool" : toolState.name;
                events.add(buildToolUseStartEvent(toolState));
                toolState.started = true;
            }
            events.add(buildToolUseDeltaEvent(toolState.contentBlockIndex, function.path("arguments").asText()));
        }
    }

    private AnthropicStreamEvent buildToolUseStartEvent(ToolCallStreamState toolState) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "content_block_start");
        event.put("index", toolState.contentBlockIndex);

        ObjectNode contentBlock = objectMapper.createObjectNode();
        contentBlock.put("type", "tool_use");
        contentBlock.put("id", toolState.id == null || toolState.id.isBlank()
                ? "toolu_proxy_" + toolState.contentBlockIndex
                : toolState.id);
        contentBlock.put("name", toolState.name);
        contentBlock.set("input", objectMapper.createObjectNode());
        event.set("content_block", contentBlock);

        return new AnthropicStreamEvent("content_block_start", writeJson(event));
    }

    private AnthropicStreamEvent buildToolUseDeltaEvent(int index, String partialJson) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "content_block_delta");
        event.put("index", index);

        ObjectNode delta = objectMapper.createObjectNode();
        delta.put("type", "input_json_delta");
        delta.put("partial_json", partialJson);
        event.set("delta", delta);

        return new AnthropicStreamEvent("content_block_delta", writeJson(event));
    }

    private void stopOpenContentBlocks(List<AnthropicStreamEvent> events, AnthropicStreamState state) {
        if (state.textBlockStarted && !state.textBlockStopped) {
            events.add(buildContentBlockStopEvent(state.textBlockIndex));
            state.textBlockStopped = true;
        }
        for (ToolCallStreamState toolState : state.toolCalls.values()) {
            if (toolState.started && !toolState.stopped) {
                events.add(buildContentBlockStopEvent(toolState.contentBlockIndex));
                toolState.stopped = true;
            }
        }
    }

    private AnthropicStreamEvent buildContentBlockStopEvent(int index) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "content_block_stop");
        event.put("index", index);
        return new AnthropicStreamEvent("content_block_stop", writeJson(event));
    }

    private AnthropicStreamEvent buildMessageDeltaEvent(String stopReason, JsonNode usageNode) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "message_delta");

        ObjectNode delta = objectMapper.createObjectNode();
        delta.put("stop_reason", stopReason);
        event.set("delta", delta);

        ObjectNode usage = objectMapper.createObjectNode();
        if (usageNode != null && !usageNode.isMissingNode() && usageNode.has("completion_tokens")) {
            usage.put("output_tokens", usageNode.path("completion_tokens").asInt());
        } else {
            usage.put("output_tokens", 0);
        }
        event.set("usage", usage);

        return new AnthropicStreamEvent("message_delta", writeJson(event));
    }

    private void appendWithSpacing(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append('\n');
        }
        sb.append(value);
    }

    private String writeJson(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 Anthropic 事件失败", e);
        }
    }

    /**
     * 从 AnthropicMessage content 中提取文本
     * content 可以是 JsonNode（字符串或数组格式）
     */
    private String extractTextFromContent(com.fasterxml.jackson.databind.JsonNode content) {
        if (content == null || content.isNull()) {
            return "";
        }
        // 纯文本格式
        if (content.isTextual()) {
            return content.asText();
        }
        // 数组格式：[{"type": "text", "text": "..."}]
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (com.fasterxml.jackson.databind.JsonNode item : content) {
                if (item.isObject()) {
                    com.fasterxml.jackson.databind.JsonNode textNode = item.get("text");
                    if (textNode != null && textNode.isTextual()) {
                        sb.append(textNode.asText());
                    }
                }
            }
            return sb.toString();
        }
        return content.toString();
    }
}
