package com.forge.dc.modules.ai.config;

import com.forge.dc.modules.ai.websocket.ChatWebSocketHandler;
import com.forge.dc.modules.ai.websocket.TaskWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskWebSocketHandler taskWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(TaskWebSocketHandler taskWebSocketHandler, ChatWebSocketHandler chatWebSocketHandler) {
        this.taskWebSocketHandler = taskWebSocketHandler;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskWebSocketHandler, "/ws/ai/task")
                .setAllowedOrigins("*");
        registry.addHandler(chatWebSocketHandler, "/ws/ai/chat")
                .setAllowedOrigins("*");
    }
}
