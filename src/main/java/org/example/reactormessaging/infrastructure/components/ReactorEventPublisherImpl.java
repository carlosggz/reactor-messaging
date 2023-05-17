package org.example.reactormessaging.infrastructure.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reactormessaging.domain.components.ReactorEventPublisher;
import org.example.reactormessaging.domain.exceptions.InvalidPayloadException;
import org.example.reactormessaging.domain.models.KafkaMessage;
import org.example.reactormessaging.domain.models.RabbitMessage;
import com.rabbitmq.client.AMQP;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.OutboundMessageResult;
import reactor.rabbitmq.SendOptions;
import reactor.rabbitmq.Sender;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReactorEventPublisherImpl implements ReactorEventPublisher {
    private final Sender sender;
    private final SendOptions sendOptions;
    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Boolean> sendMessage(RabbitMessage rabbitMessage) {
        return Mono
                .just(payloadToString(rabbitMessage.getPayload()))
                .map(payload -> new OutboundMessage(
                        rabbitMessage.getExchangeName(),
                        rabbitMessage.getRoutingKey(),
                        new AMQP.BasicProperties.Builder().build(),
                        payload.getBytes(StandardCharsets.UTF_8)))
                .transform(x -> sender.sendWithPublishConfirms(x, sendOptions))
                .map(OutboundMessageResult::isAck);
    }

    public Mono<Boolean> sendMessage(KafkaMessage kafkaMessage) {
        return Mono
                .just(payloadToString(kafkaMessage.getPayload()))
                .map(payload -> new ProducerRecord<>(
                        kafkaMessage.getTopic(),
                        kafkaMessage.getRoutingKey(),
                        payload))
                .map(producerRecord -> SenderRecord.create(producerRecord, producerRecord.key()))
                .as(kafkaSender::send)
                .map(result -> Objects.isNull(result.exception()))
                .next();
    }

    private String payloadToString(Object payload) {
        try {
            return payload instanceof String
                    ? payload.toString()
                    : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new InvalidPayloadException("Error parsing payload: " + e.getMessage(), e);
        }
    }
}
