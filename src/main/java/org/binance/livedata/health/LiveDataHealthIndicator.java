
package org.binance.livedata.health;

import lombok.RequiredArgsConstructor;
import org.binance.livedata.websocket.LiveDataSocketHandler;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LiveDataHealthIndicator implements HealthIndicator {

    private final LiveDataSocketHandler socketHandler;

    @Override
    public Health health() {
        Map<String, Set<WebSocketSession>> sessions = socketHandler.getSessionsBySymbol();

        int totalSessions = sessions.values().stream()
                .mapToInt(Set::size)
                .sum();

        Map<String, Integer> sessionsPerSymbol = new HashMap<>();
        sessions.forEach((symbol, set) -> sessionsPerSymbol.put(symbol, set.size()));

        return Health.up()
                .withDetail("totalActiveSessions", totalSessions)
                .withDetail("sessionsPerSymbol", sessionsPerSymbol)
                .withDetail("trackedSymbols", sessions.keySet().size())
                .build();
    }
}