package org.binance.livedata.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveDataSocketHandler implements WebSocketHandler {

    private final JsonMapper jsonMapper;

    Map<String, Set<WebSocketSession>> sessionsBySymbol = new ConcurrentHashMap<>();

    private String extractSymbolFromUri(@Nullable URI uri) {
        if (uri == null) {
            return null;
        }
        String prefix = "/ws/live@";
        if (uri.getPath().startsWith(prefix)) {
            return uri.getPath().substring(prefix.length()).toUpperCase();
        }
        return null;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String symbol = this.extractSymbolFromUri(session.getUri());
        Set<WebSocketSession> sessions = sessionsBySymbol.get(symbol);
        if (sessions == null) {
            sessions = ConcurrentHashMap.newKeySet();
            sessionsBySymbol.put(symbol, sessions);
        }
        sessions.add(session);
    }


    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        log.info("Received message: {}", message.getPayload());
        log.info("From session: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String symbol = this.extractSymbolFromUri(session.getUri());
        Set<WebSocketSession> symbolSessions = sessionsBySymbol.get(symbol);
        if (symbolSessions != null) {
            symbolSessions.remove(session);
            if (symbolSessions.isEmpty()) {
                sessionsBySymbol.remove(symbol);
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void broadcastMessage(String message) {

        String symbol;
        try {
            symbol = jsonMapper.readTree(message).get("s").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Set<WebSocketSession> sessions = sessionsBySymbol.get(symbol);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
