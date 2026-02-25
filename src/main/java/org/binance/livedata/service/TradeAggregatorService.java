package org.binance.livedata.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.binance.livedata.model.LiveCandle;
import org.binance.livedata.websocket.LiveDataSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TradeAggregatorService {

    private final JsonMapper jsonMapper;
    private final ObjectMapper objectMapper;
    private final LiveDataSocketHandler socketHandler;

//    Metrics
    private final Counter candlesBroadcastCounter;
    private final Timer broadCastTimer;


    @Value("${live.aggregation.interval-ms:1000}")
    private long intervalMs;

    private final Map<String, LiveCandle> candles = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    public TradeAggregatorService(
            JsonMapper jsonMapper,
            ObjectMapper objectMapper,
            LiveDataSocketHandler socketHandler,
            MeterRegistry meterRegistry
    ) {
        this.jsonMapper = jsonMapper;
        this.objectMapper = objectMapper;
        this.socketHandler = socketHandler;
        this.candlesBroadcastCounter = Counter.builder("live.candles.broadcast")
                .description("Total aggregated candles broadcast to clients")
                .register(meterRegistry);
        Gauge.builder("live.symbols.tracked", candles, Map::size)
                .description("Number of symbols currently being aggregated")
                .register(meterRegistry);
        broadCastTimer = Timer.builder("live.candle.broadcast.duration")
                .description("Total duration of candles broadcast to clients")
                .register(meterRegistry);

    }

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::broadcastAllCandles,
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS
        );
        log.info("TradeAggregatorService started with {}ms interval", intervalMs);
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void processTrade(String tradeJson) {
        try {
            var node = jsonMapper.readTree(tradeJson);

            String symbol = node.get("s").asText();
            double price = node.get("p").asDouble();
            double quantity = node.get("q").asDouble();
            long tradeTime = node.get("T").asLong();

            LiveCandle candle = candles.computeIfAbsent(symbol, s -> {
                LiveCandle c = new LiveCandle();
                c.setSymbol(s);
                return c;
            });

            candle.addTrade(price, quantity, tradeTime);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse trade JSON: {}", tradeJson, e);
        }
    }

    private void broadcastAllCandles() {
        try {
            broadCastTimer.record(() -> {
                for (Map.Entry<String, LiveCandle> entry : candles.entrySet()) {
                    LiveCandle candle = entry.getValue();

                    if (candle.getTradeCount() == 0) {
                        continue;
                    }

                    LiveCandle snapshot = candle.snapshot();
                    candle.reset();

                    try {
                        String json = objectMapper.writeValueAsString(snapshot);
                        socketHandler.broadcastToSymbol(snapshot.getSymbol(), json);
                        candlesBroadcastCounter.increment();
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize candle for {}", snapshot.getSymbol(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error during candle broadcast", e);
        }
    }
}