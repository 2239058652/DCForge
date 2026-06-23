package com.forge.dc.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.forge.dc.modules.ai.dto.AnthropicRequestDTO;
import com.forge.dc.modules.ai.dto.AnthropicResponseDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionRequestDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionResponseDTO;
import com.forge.dc.modules.ai.dto.ChatMessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgnesProxyService 单元测试
 */
class AgnesProxyServiceTest {

    private AgnesProxyService agnesProxyService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        agnesProxyService = new AgnesProxyService(objectMapper);
    }

    private JsonNode json(String value) {
        return objectMapper.valueToTree(value);
    }

    @Test
    void convertAnthropicToOpenAI_basicConversion() {
        // 准备 Anthropic 格式请求
        AnthropicRequestDTO anthropicRequest = new AnthropicRequestDTO();
        anthropicRequest.setModel("claude-3-sonnet");
        anthropicRequest.setMaxTokens(1024);
        anthropicRequest.setSystem(json("You are a helpful assistant"));
        anthropicRequest.setTemperature(0.7);
        anthropicRequest.setStream(false);

        AnthropicRequestDTO.AnthropicMessage userMsg = new AnthropicRequestDTO.AnthropicMessage();
        userMsg.setRole("user");
        userMsg.setContent(json("Hello!"));
        anthropicRequest.setMessages(List.of(userMsg));

        // 执行转换
        ChatCompletionRequestDTO openAIRequest = agnesProxyService.convertAnthropicToOpenAI(anthropicRequest);

        // 验证转换结果
        assertNotNull(openAIRequest);
        assertEquals("You are a helpful assistant", openAIRequest.getSystemPrompt());
        assertNotNull(openAIRequest.getMessages());
        assertEquals(1, openAIRequest.getMessages().size());
        assertEquals("user", openAIRequest.getMessages().get(0).getRole());
        assertEquals("Hello!", openAIRequest.getMessages().get(0).getContent());
        assertEquals(0.7, openAIRequest.getTemperature());
        assertEquals(1024, openAIRequest.getMaxTokens());
        assertEquals(false, openAIRequest.getStream());
    }

    @Test
    void convertAnthropicToOpenAI_multipleMessages() {
        // 准备多轮对话
        AnthropicRequestDTO anthropicRequest = new AnthropicRequestDTO();
        anthropicRequest.setModel("claude-3-sonnet");
        anthropicRequest.setSystem(json("You are a helpful assistant"));

        AnthropicRequestDTO.AnthropicMessage userMsg1 = new AnthropicRequestDTO.AnthropicMessage();
        userMsg1.setRole("user");
        userMsg1.setContent(json("First message"));

        AnthropicRequestDTO.AnthropicMessage assistantMsg1 = new AnthropicRequestDTO.AnthropicMessage();
        assistantMsg1.setRole("assistant");
        assistantMsg1.setContent(json("First response"));

        AnthropicRequestDTO.AnthropicMessage userMsg2 = new AnthropicRequestDTO.AnthropicMessage();
        userMsg2.setRole("user");
        userMsg2.setContent(json("Second message"));

        anthropicRequest.setMessages(Arrays.asList(userMsg1, assistantMsg1, userMsg2));

        // 执行转换
        ChatCompletionRequestDTO openAIRequest = agnesProxyService.convertAnthropicToOpenAI(anthropicRequest);

        // 验证
        assertEquals(3, openAIRequest.getMessages().size());
        assertEquals("user", openAIRequest.getMessages().get(0).getRole());
        assertEquals("assistant", openAIRequest.getMessages().get(1).getRole());
        assertEquals("user", openAIRequest.getMessages().get(2).getRole());
    }

    @Test
    void convertAnthropicToOpenAI_systemInMessages() {
        // Anthropic 不支持 system role，如果出现应该合并到 system prompt
        AnthropicRequestDTO anthropicRequest = new AnthropicRequestDTO();
        anthropicRequest.setSystem(json("Initial system prompt"));

        AnthropicRequestDTO.AnthropicMessage systemMsg = new AnthropicRequestDTO.AnthropicMessage();
        systemMsg.setRole("system");
        systemMsg.setContent(json("Additional system instruction"));

        AnthropicRequestDTO.AnthropicMessage userMsg = new AnthropicRequestDTO.AnthropicMessage();
        userMsg.setRole("user");
        userMsg.setContent(json("Hello"));

        anthropicRequest.setMessages(Arrays.asList(systemMsg, userMsg));

        // 执行转换
        ChatCompletionRequestDTO openAIRequest = agnesProxyService.convertAnthropicToOpenAI(anthropicRequest);

        // 验证 system prompt 被合并
        assertTrue(openAIRequest.getSystemPrompt().contains("Initial system prompt"));
        assertTrue(openAIRequest.getSystemPrompt().contains("Additional system instruction"));
        // messages 中应该只有 user 消息
        assertEquals(1, openAIRequest.getMessages().size());
        assertEquals("user", openAIRequest.getMessages().get(0).getRole());
    }

    @Test
    void convertAnthropicToOpenAI_toolUseAndToolResult() throws Exception {
        AnthropicRequestDTO anthropicRequest = new AnthropicRequestDTO();
        anthropicRequest.setTools(objectMapper.readTree("""
                [
                  {
                    "name": "read_file",
                    "description": "Read a local file",
                    "input_schema": {
                      "type": "object",
                      "properties": {
                        "path": {"type": "string"}
                      },
                      "required": ["path"]
                    }
                  }
                ]
                """));

        AnthropicRequestDTO.AnthropicMessage userMsg = new AnthropicRequestDTO.AnthropicMessage();
        userMsg.setRole("user");
        userMsg.setContent(json("Read pom.xml"));

        AnthropicRequestDTO.AnthropicMessage assistantMsg = new AnthropicRequestDTO.AnthropicMessage();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(objectMapper.readTree("""
                [
                  {"type": "text", "text": "I will read it."},
                  {
                    "type": "tool_use",
                    "id": "toolu_1",
                    "name": "read_file",
                    "input": {"path": "pom.xml"}
                  }
                ]
                """));

        AnthropicRequestDTO.AnthropicMessage resultMsg = new AnthropicRequestDTO.AnthropicMessage();
        resultMsg.setRole("user");
        resultMsg.setContent(objectMapper.readTree("""
                [
                  {
                    "type": "tool_result",
                    "tool_use_id": "toolu_1",
                    "content": [{"type": "text", "text": "project xml"}]
                  }
                ]
                """));

        anthropicRequest.setMessages(List.of(userMsg, assistantMsg, resultMsg));

        ChatCompletionRequestDTO openAIRequest = agnesProxyService.convertAnthropicToOpenAI(anthropicRequest);

        assertEquals("function", openAIRequest.getTools().get(0).get("type").asText());
        assertEquals("read_file", openAIRequest.getTools().get(0).get("function").get("name").asText());
        assertEquals(3, openAIRequest.getMessages().size());
        assertEquals("assistant", openAIRequest.getMessages().get(1).getRole());
        assertEquals("toolu_1", openAIRequest.getMessages().get(1).getToolCalls().get(0).get("id").asText());
        assertEquals("tool", openAIRequest.getMessages().get(2).getRole());
        assertEquals("toolu_1", openAIRequest.getMessages().get(2).getToolCallId());
        assertEquals("project xml", openAIRequest.getMessages().get(2).getContent());
    }

    @Test
    void convertOpenAIToAnthropic_basicConversion() throws Exception {
        // 准备 OpenAI 格式响应
        ChatCompletionResponseDTO openAIResponse = new ChatCompletionResponseDTO();
        openAIResponse.setId("chatcmpl-123");
        openAIResponse.setModel("agnes-2.0-flash");

        ChatCompletionResponseDTO.Message message = new ChatCompletionResponseDTO.Message();
        message.setRole("assistant");
        message.setContent("Hello! How can I help you?");

        ChatCompletionResponseDTO.Choice choice = new ChatCompletionResponseDTO.Choice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason("stop");
        openAIResponse.setChoices(List.of(choice));

        ChatCompletionResponseDTO.Usage usage = new ChatCompletionResponseDTO.Usage();
        usage.setPromptTokens(15);
        usage.setCompletionTokens(12);
        usage.setTotalTokens(27);
        openAIResponse.setUsage(usage);

        // 执行转换
        AnthropicResponseDTO anthropicResponse = agnesProxyService.convertOpenAIToAnthropic(openAIResponse);

        // 验证转换结果
        assertNotNull(anthropicResponse);
        assertEquals("chatcmpl-123", anthropicResponse.getId());
        assertEquals("message", anthropicResponse.getType());
        assertEquals("agnes-2.0-flash", anthropicResponse.getModel());
        assertEquals("assistant", anthropicResponse.getRole());
        assertEquals("end_turn", anthropicResponse.getStopReason());

        assertNotNull(anthropicResponse.getContent());
        assertEquals(1, anthropicResponse.getContent().size());
        assertEquals("text", anthropicResponse.getContent().get(0).getType());
        assertEquals("Hello! How can I help you?", anthropicResponse.getContent().get(0).getText());

        assertNotNull(anthropicResponse.getUsage());
        assertEquals(15, anthropicResponse.getUsage().getInputTokens());
        assertEquals(12, anthropicResponse.getUsage().getOutputTokens());
    }

    @Test
    void convertOpenAIToAnthropic_toolCallConversion() throws Exception {
        ChatCompletionResponseDTO openAIResponse = new ChatCompletionResponseDTO();
        openAIResponse.setId("chatcmpl-tool");
        openAIResponse.setModel("agnes-2.0-flash");

        ChatCompletionResponseDTO.Message message = new ChatCompletionResponseDTO.Message();
        message.setRole("assistant");
        message.setToolCalls(objectMapper.readTree("""
                [
                  {
                    "id": "call_1",
                    "type": "function",
                    "function": {
                      "name": "read_file",
                      "arguments": "{\\"path\\":\\"pom.xml\\"}"
                    }
                  }
                ]
                """));

        ChatCompletionResponseDTO.Choice choice = new ChatCompletionResponseDTO.Choice();
        choice.setMessage(message);
        choice.setFinishReason("tool_calls");
        openAIResponse.setChoices(List.of(choice));

        AnthropicResponseDTO anthropicResponse = agnesProxyService.convertOpenAIToAnthropic(openAIResponse);

        assertEquals("tool_use", anthropicResponse.getContent().get(0).getType());
        assertEquals("call_1", anthropicResponse.getContent().get(0).getId());
        assertEquals("read_file", anthropicResponse.getContent().get(0).getName());
        assertEquals("pom.xml", anthropicResponse.getContent().get(0).getInput().get("path").asText());
        assertEquals("tool_use", anthropicResponse.getStopReason());
    }

    @Test
    void buildAnthropicStreamStart_validOutput() throws Exception {
        String startEvent = agnesProxyService.buildAnthropicStreamStart("agnes-2.0-flash");

        assertNotNull(startEvent);
        var jsonNode = objectMapper.readTree(startEvent);
        assertEquals("message_start", jsonNode.get("type").asText());
        assertNotNull(jsonNode.get("message"));
        assertEquals("agnes-2.0-flash", jsonNode.get("message").get("model").asText());
        assertEquals("message", jsonNode.get("message").get("type").asText());
        assertEquals("assistant", jsonNode.get("message").get("role").asText());
    }

    @Test
    void buildAnthropicStreamEnd_validOutput() throws Exception {
        String endEvent = agnesProxyService.buildAnthropicStreamEnd();

        assertNotNull(endEvent);
        var jsonNode = objectMapper.readTree(endEvent);
        assertEquals("message_stop", jsonNode.get("type").asText());
    }

    @Test
    void convertOpenAIStreamChunkToAnthropicEvents_textSequence() throws Exception {
        AgnesProxyService.AnthropicStreamState state = new AgnesProxyService.AnthropicStreamState();

        String contentChunk = "{\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hello\"},\"finish_reason\":null}]}";
        List<AgnesProxyService.AnthropicStreamEvent> contentEvents =
                agnesProxyService.convertOpenAIStreamChunkToAnthropicEvents(contentChunk, state);

        assertEquals(2, contentEvents.size());
        assertEquals("content_block_start", contentEvents.get(0).event());
        assertEquals("content_block_delta", contentEvents.get(1).event());

        String finishChunk = "{\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}";
        List<AgnesProxyService.AnthropicStreamEvent> finishEvents =
                agnesProxyService.convertOpenAIStreamChunkToAnthropicEvents(finishChunk, state);

        assertEquals("content_block_stop", finishEvents.get(0).event());
        assertEquals("message_delta", finishEvents.get(1).event());
        var messageDelta = objectMapper.readTree(finishEvents.get(1).data());
        assertEquals("end_turn", messageDelta.get("delta").get("stop_reason").asText());

        List<AgnesProxyService.AnthropicStreamEvent> endEvents =
                agnesProxyService.buildAnthropicStreamEndEvents(state);
        assertEquals(1, endEvents.size());
        assertEquals("message_stop", endEvents.get(0).event());
    }

    @Test
    void roundTripConversion() throws Exception {
        // 完整的往返转换测试
        AnthropicRequestDTO originalRequest = new AnthropicRequestDTO();
        originalRequest.setModel("claude-3-sonnet");
        originalRequest.setMaxTokens(2048);
        originalRequest.setSystem(json("Be helpful"));
        originalRequest.setTemperature(0.5);
        originalRequest.setStream(false);

        AnthropicRequestDTO.AnthropicMessage msg1 = new AnthropicRequestDTO.AnthropicMessage();
        msg1.setRole("user");
        msg1.setContent(json("Test message"));

        AnthropicRequestDTO.AnthropicMessage msg2 = new AnthropicRequestDTO.AnthropicMessage();
        msg2.setRole("assistant");
        msg2.setContent(json("Test response"));

        originalRequest.setMessages(Arrays.asList(msg1, msg2));

        // Anthropic -> OpenAI
        ChatCompletionRequestDTO openAIRequest = agnesProxyService.convertAnthropicToOpenAI(originalRequest);

        // 验证中间结果
        assertEquals("Be helpful", openAIRequest.getSystemPrompt());
        assertEquals(0.5, openAIRequest.getTemperature());
        assertEquals(2048, openAIRequest.getMaxTokens());
        assertEquals(2, openAIRequest.getMessages().size());

        // 构造 OpenAI 响应
        ChatCompletionResponseDTO openAIResponse = new ChatCompletionResponseDTO();
        openAIResponse.setId("test-123");
        openAIResponse.setModel("agnes-2.0-flash");

        ChatCompletionResponseDTO.Message msg = new ChatCompletionResponseDTO.Message();
        msg.setRole("assistant");
        msg.setContent("Response text");

        ChatCompletionResponseDTO.Choice choice = new ChatCompletionResponseDTO.Choice();
        choice.setMessage(msg);
        choice.setFinishReason("stop");
        openAIResponse.setChoices(List.of(choice));

        // OpenAI -> Anthropic
        AnthropicResponseDTO anthropicResponse = agnesProxyService.convertOpenAIToAnthropic(openAIResponse);

        // 验证最终结果
        assertEquals("test-123", anthropicResponse.getId());
        assertEquals("Response text", anthropicResponse.getContent().get(0).getText());
        assertEquals("end_turn", anthropicResponse.getStopReason());
    }
}
