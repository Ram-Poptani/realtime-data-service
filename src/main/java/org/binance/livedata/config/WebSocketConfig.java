package org.binance.livedata.config;

import lombok.RequiredArgsConstructor;
import org.binance.livedata.websocket.LiveDataSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${websocket.allowed-origins}")
    private String allowedOrigins;

    private final LiveDataSocketHandler liveDataSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(
                this.liveDataSocketHandler,
        "ws/live"
        ).setAllowedOrigins(allowedOrigins.split(","));
    }
}
