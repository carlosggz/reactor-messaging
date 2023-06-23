package org.example.reactormessaging.infrastructure.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.example.reactormessaging.domain.components.ReactorEventPublisher;
import org.example.reactormessaging.domain.exceptions.InvalidPayloadException;
import org.example.reactormessaging.domain.models.KafkaMessage;
import org.example.reactormessaging.domain.models.RabbitMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.rabbitmq.OutboundMessageResult;
import reactor.rabbitmq.SendOptions;
import reactor.rabbitmq.Sender;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

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
                .just(objectToString(rabbitMessage.getPayload()))
                .map(payload -> new OutboundMessage(
                        rabbitMessage.getExchangeName(),
                        rabbitMessage.getRoutingKey(),
                        new AMQP.BasicProperties.Builder().headers(rabbitMessage.getHeaders()).build(),
                        payload.getBytes(StandardCharsets.UTF_8)))
                .transform(x -> sender.sendWithPublishConfirms(x, sendOptions))
                .map(OutboundMessageResult::isAck);
    }

    public Mono<Boolean> sendMessage(KafkaMessage kafkaMessage) {
        var headers = kafkaMessage.getHeaders()
                .entrySet()
                .stream()
                .map(x -> new RecordHeader(x.getKey(), objectToString(x.getValue()).getBytes()))
                .map(x -> (Header)x)
                .collect(Collectors.toList());

        return Mono
                .just(objectToString(kafkaMessage.getPayload()))
                .map(payload -> new ProducerRecord<>(
                        kafkaMessage.getTopic(),
                        null,
                        kafkaMessage.getRoutingKey(),
                        payload,
                        headers))
                .map(producerRecord -> SenderRecord.create(producerRecord, producerRecord.key()))
                .as(kafkaSender::send)
                .map(result -> Objects.isNull(result.exception()))
                .next();
    }

    private String objectToString(Object payload) {
        try {
            return payload instanceof String
                    ? payload.toString()
                    : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new InvalidPayloadException("Error parsing payload: " + e.getMessage(), e);
        }
    }
}
