package org.example.reactormessaging.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@TestConfiguration
@Slf4j
class ConsumersConfig {
    private final List<String> successCollection = Collections.synchronizedList(new ArrayList<>());

    @Bean("testSuccessConcurrentCollection")
    public List<String> successCollection() {
        return successCollection;
    }

    @Bean
    public Consumer<Message<String>> fromRouted() {
        return message -> registerMessage(message.getPayload(), "fromRouted");
    }

    @Bean
    public Consumer<Message<String>> fromCompleted() {
        return message -> registerMessage(message.getPayload(), "fromCompleted");
    }

    @Bean
    public Consumer<Message<String>> fromRejected() {
        return message -> registerMessage(message.getPayload(), "fromRejected");
    }

    @Bean
    public Consumer<Message<CustomDto>> fromCustom() {
        return message -> registerMessage(message.getPayload().toString(), "fromCustom");
    }

    @Bean
    public Consumer<Message<String>> fromEventsKafka()  {
        return message -> registerMessage(message.getPayload(), "fromEventsKafka");
    }

    @Bean
    public Consumer<Message<CustomDto>> fromCustomKafka() {
        return message -> registerMessage(message.getPayload().toString(), "fromCustomKafka");
    }

    @Bean
    public Consumer<Message<String>> fromError()  {
        return message -> {
            registerMessage(message.getPayload(), "fromError");
            throw new RuntimeException("Just testing");
        };
    }

    private void registerMessage(String value, String consumerName) {
        log.info("Entered into the {} consumer with value: {}", consumerName, value);
        successCollection.add(value);
    }
}
