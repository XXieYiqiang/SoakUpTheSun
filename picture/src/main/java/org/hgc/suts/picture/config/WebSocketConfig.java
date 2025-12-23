package org.hgc.suts.picture.config;

import lombok.RequiredArgsConstructor;
import org.hgc.suts.picture.ws.handler.AiImageBinaryHandler;
import org.hgc.suts.picture.ws.interceptor.AiAuthHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiImageBinaryHandler aiImageBinaryHandler;
    private final AiAuthHandshakeInterceptor aiAuthHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiImageBinaryHandler, "/ws/ai/video")
                .addInterceptors(aiAuthHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}