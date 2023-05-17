package org.example.reactormessaging.domain.components;

import org.example.reactormessaging.domain.models.KafkaMessage;
import org.example.reactormessaging.domain.models.RabbitMessage;
import reactor.core.publisher.Mono;

/**
 * Events publisher
 */
public interface ReactorEventPublisher {
    /**
     * Sends a message using a rabbit exchange
     * @param rabbitMessage The message settings
     * @return true is message was successfully sent
     */
    Mono<Boolean> sendMessage(RabbitMessage rabbitMessage);

    /**
     * Sends a message to a kafka topic
     * @param kafkaMessage The message settings
     * @return true is message was successfully sent
     */
    Mono<Boolean> sendMessage(KafkaMessage kafkaMessage);
}
