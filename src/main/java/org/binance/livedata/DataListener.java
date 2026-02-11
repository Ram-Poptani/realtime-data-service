package org.binance.livedata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.binance.livedata.service.TradeAggregatorService;
import org.binance.livedata.websocket.LiveDataSocketHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class DataListener {

//    private final LiveDataSocketHandler liveDataSocketHandler;
//
//    DataListener(LiveDataSocketHandler liveDataSocketHandler) {
//        this.liveDataSocketHandler = liveDataSocketHandler;
//        log.info("DataListener initialized with LiveDataSocketHandler");
//    }

    private final TradeAggregatorService tradeAggregatorService;


    @Transactional
    @RabbitListener(queues = "${rabbit-mq.consumer.queue}")
    public void storeData(String data) {
        try {
            tradeAggregatorService.processTrade(data);
        } catch (Exception e) {
            log.error("Error sending data: {}", data, e);
            throw new RuntimeException("Failed to send data: " + data, e);
        }
    }

}
