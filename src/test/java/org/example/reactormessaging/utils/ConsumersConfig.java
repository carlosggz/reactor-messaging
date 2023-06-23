package org.example.reactormessaging.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.reactormessaging.domain.models.MessageDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@TestConfiguration
@Slf4j
class ConsumersConfig {
    private final List<MessageDetails<?>> successCollection = Collections.synchronizedList(new ArrayList<>());

    @Bean("testSuccessConcurrentCollection")
    public List<MessageDetails<?>> successCollection() {
        return successCollection;
    }

    @Bean
    public Consumer<MessageDetails<String>> fromRouted() {
        return message -> registerMessage(message, "fromRouted", message.getRoutingKey());
    }

    @Bean
    public Consumer<MessageDetails<String>> fromCompleted() {
        return message -> registerMessage(message, "fromCompleted", message.getRoutingKey());
    }

    @Bean
    public Consumer<MessageDetails<String>> fromRejected() {
        return message -> registerMessage(message, "fromRejected", message.getRoutingKey());
    }

    @Bean
    public Consumer<MessageDetails<CustomDto>> fromCustom() {
        return message -> registerMessage(message, "fromCustom", message.getRoutingKey());
    }

    @Bean
    public Consumer<MessageDetails<String>> fromEventsKafka()  {
        return message -> registerMessage(message, "fromEventsKafka", message.getRoutingKey());
    }

    @Bean
    public Consumer<MessageDetails<CustomDto>> fromCustomKafka() {
        return message -> registerMessage(message, "fromCustomKafka", message.getRoutingKey());
    }

    @Bean
    public Consumer<MessageDetails<String>> fromError()  {
        return message -> {
            registerMessage(message, "fromError", message.getRoutingKey());
            throw new RuntimeException("Just testing");
        };
    }

    private void registerMessage(MessageDetails<?> value, String consumerName, String routingKey) {
        log.info("Entered into the {} consumer with value: {} and routing key {}", consumerName, value, routingKey);
        successCollection.add(value);
    }
}

