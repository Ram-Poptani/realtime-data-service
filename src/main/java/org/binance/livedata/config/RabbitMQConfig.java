package org.binance.livedata.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${rabbit-mq.default.exchange}")
    private String exchange;

    @Value("${rabbit-mq.consumer.queue}")
    private String queue;

    @Bean
    public FanoutExchange fanoutExchange() {
        log.info("Creating FanoutExchange: {}", exchange);
        return new FanoutExchange(exchange, true, false);
    }

    @Bean
    public Queue queue() {
        log.info("Creating Queue: {}", queue);
        return new Queue(queue, true, false, false);
    }

    @Bean
    public Binding binding(Queue queue, FanoutExchange fanoutExchange) {
        log.info("Binding Queue '{}' to FanoutExchange '{}'", queue.getName(), exchange);
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }
}