package com.forge.dc.modules.ai.service;

import com.forge.dc.modules.ai.dto.ChatCompletionRequestDTO;
import com.forge.dc.modules.ai.dto.ChatCompletionResponseDTO;
import com.forge.dc.modules.ai.dto.StreamChatRequestDTO;
import okhttp3.Call;

import java.io.IOException;

public interface ChatCompletionService {

    /**
     * 同步聊天补全
     */
    ChatCompletionResponseDTO chatCompletion(ChatCompletionRequestDTO request) throws IOException;

    /**
     * 流式聊天补全
     * @return OkHttp Call 对象，可用于取消请求
     */
    Call streamChatCompletion(StreamChatRequestDTO request, StreamCallback callback) throws IOException;

    /**
     * 流式回调接口
     */
    interface StreamCallback {
        void onMessage(String content);

        void onComplete(String fullContent);

        void onError(String errorMessage);
    }
}
