package org.binance.livedata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.binance.livedata.websocket.LiveDataSocketHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
public class DataListener {

    private final LiveDataSocketHandler liveDataSocketHandler;

    DataListener(LiveDataSocketHandler liveDataSocketHandler) {
        this.liveDataSocketHandler = liveDataSocketHandler;
        log.info("DataListener initialized with LiveDataSocketHandler");
    }

    @Transactional
    @RabbitListener(queues = "${rabbit-mq.consumer.queue}")
    public void storeData(String data) {
        try {
            liveDataSocketHandler.broadcastMessage(data);
        } catch (Exception e) {
            log.error("Error sending data: {}", data, e);
            throw new RuntimeException("Failed to send data: " + data, e);
        }
    }

}
