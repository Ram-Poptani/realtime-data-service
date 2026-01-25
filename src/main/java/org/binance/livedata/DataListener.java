package org.binance.livedata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class DataListener {


    private final JsonMapper jsonMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    @RabbitListener(queues = "${rabbit-mq.consumer.queue}")
    public void storeData(String data) {
        try {

            JsonNode jsonNode = jsonMapper.readTree(data);



        } catch (Exception e) {
            log.error("Error processing data: {}", data, e);
            throw new RuntimeException("Failed to process data: " + data, e);
        }
    }

}
