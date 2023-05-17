package org.example.reactormessaging.infrastructure.config;

import com.fasterxml.jackson.databind.JavaType;
import org.example.reactormessaging.domain.components.ReflectionUtilsComponent;
import org.example.reactormessaging.domain.exceptions.RetryExhaustedException;
import org.example.reactormessaging.infrastructure.properties.ReactorKafkaProperties;
import org.example.reactormessaging.infrastructure.properties.ReactorProperties;
import org.example.reactormessaging.infrastructure.properties.ReactorRabbitProperties;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static reactor.rabbitmq.BindingSpecification.binding;
import static reactor.rabbitmq.ExchangeSpecification.exchange;
import static reactor.rabbitmq.QueueSpecification.queue;


@Slf4j
@Configuration
@RequiredArgsConstructor
@ComponentScan("org.example.reactormessaging")
public class ReactorStreamConfig {
    private static final String DEAD_LETTER_EXCHANGE = "DLX";
    private static final String DEAD_LETTER_QUEUE = ".dlq";

    private final ReflectionUtilsComponent reflectionUtilsComponent;

    @Bean
    public RetryBackoffSpec retrySpec(ReactorProperties reactorProperties) {
        return Retry
                .backoff(reactorProperties.getRetryAttempts(), Duration.ofMillis(reactorProperties.getRetryDuration()))
                .doBeforeRetry(x -> log.info("Retrying ({}) due to error: {}", x.totalRetries()+1, x.failure().getMessage()))
                .onRetryExhaustedThrow((signal, error) -> new RetryExhaustedException(
                        "Reached the max of retries",
                        error.failure()));
    }

    @Bean
    public List<DisposableBean> createOrValidateRabbitMQTopology(
            Sender sender,
            Receiver receiver,
            BiConsumer<Receiver.AcknowledgmentContext, Exception> connectionRecovery,
            ReactorRabbitProperties reactorRabbitProperties,
            RetryBackoffSpec retrySpec) {

        val bindings = Optional
                .ofNullable(reactorRabbitProperties.getBindings())
                .orElse(Map.of())
                .entrySet();

        bindings.forEach(q ->
                registerExchange(sender, q.getValue())
                        .flatMap(b -> registerQueue(q.getValue(), sender))
                        .flatMap(b -> registerDeadLetter(q.getValue(), sender))
                        .block());
        return bindings
                .stream()
                .map(q -> registerRabbitConsumerBean(q.getKey(), q.getValue(), receiver, connectionRecovery, retrySpec))
                .collect(Collectors.toList());
    }

    @Bean
    public List<DisposableBean> createOrValidateKafkaTopology(
            ReactorKafkaProperties reactorKafkaProperties,
            reactor.kafka.receiver.ReceiverOptions<Integer, String> receiverOptions,
            RetryBackoffSpec retrySpec) {

        return Optional
                .ofNullable(reactorKafkaProperties.getBindings())
                .orElse(Map.of())
                .entrySet()
                .stream()
                .map(q -> registerKafkaConsumerBean(q.getKey(), q.getValue(), receiverOptions, retrySpec))
                .collect(Collectors.toList());
    }

    private DisposableBean registerKafkaConsumerBean(
            String consumerName,
            ReactorKafkaProperties.BindingSettings bindingSettings,
            reactor.kafka.receiver.ReceiverOptions<Integer, String> receiverOptions,
            RetryBackoffSpec retrySpec) {
        val consumer = reflectionUtilsComponent.getConsumer(consumerName);
        val targetJavaType = reflectionUtilsComponent.getJavaType(bindingSettings.getTargetType()).orElse(null);
        val options = receiverOptions.subscription(Collections.singleton(bindingSettings.getTopic()));

        var disposable = KafkaReceiver
                .create(options)
                .receive()
                .subscribe(record -> Mono
                        .just(record.value())
                        .publishOn(Schedulers.boundedElastic())
                        .map(message -> createMessageFromKafkaRecord(targetJavaType, record, message))
                        .doOnNext(consumer)
                        .retryWhen(retrySpec)
                        .doFinally(signalType -> record.receiverOffset().acknowledge())
                        .subscribe()
                );
        return disposable::dispose;
    }
    private Mono<AMQP.Exchange.DeclareOk> registerExchange(Sender sender, ReactorRabbitProperties.BindingSettings e) {
        return sender
                .declare(exchange(e.getExchangeName()).type(e.getExchangeType().name()))
                .doOnNext(bind -> log.info("Created exchange {}", e.getExchangeName()));
    }

    private Mono<AMQP.Queue.BindOk> registerQueue(ReactorRabbitProperties.BindingSettings q, Sender sender) {
        return sender
                .declare(exchange(q.getExchangeName()).type(q.getExchangeType().name()))
                .then(sender.declare(queue(q.getQueueName())
                        .durable(q.isDurable())
                        .autoDelete(q.isAutoDelete())
                        .arguments(q.isUseDlq() ? deadLetterConfig(q.getQueueName() ) : Map.of())))
                .then(sender.bind(binding().exchange(q.getExchangeName()).queue(q.getQueueName()).routingKey(q.getRoutingKey())))
                .doOnNext(bind -> log.info("Exchange {} and queue {} declared and bound with routing key {}",
                                q.getExchangeName(), q.getQueueName(), q.getRoutingKey()));
    }

    private Mono<AMQP.Queue.BindOk> registerDeadLetter(ReactorRabbitProperties.BindingSettings q, Sender sender) {

        if (!q.isUseDlq()) {
            return Mono.empty();
        }

        val dlqName = q.getQueueName() + DEAD_LETTER_QUEUE;

        return sender
                .declare(exchange(DEAD_LETTER_EXCHANGE).durable(true))
                .then(sender.declare(queue(dlqName).durable(q.isDurable())))
                .then(sender.bind(binding().exchange(DEAD_LETTER_EXCHANGE).queue(dlqName).routingKey(q.getQueueName())))
                .doOnNext(bind -> log.info("Created dlq {}", dlqName));
    }

    private DisposableBean registerRabbitConsumerBean(
            String consumerName,
            ReactorRabbitProperties.BindingSettings bindingSettings,
            Receiver receiver,
            BiConsumer<Receiver.AcknowledgmentContext, Exception> connectionRecovery,
            RetryBackoffSpec retrySpec) {
        val consumer = reflectionUtilsComponent.getConsumer(consumerName);
        val targetJavaType = reflectionUtilsComponent.getJavaType(bindingSettings.getTargetType()).orElse(null);

        var flux = bindingSettings.isAutoAck()
                ? receiver.consumeAutoAck(
                        bindingSettings.getQueueName(), new ConsumeOptions().exceptionHandler(connectionRecovery))
                : receiver.consumeManualAck(
                        bindingSettings.getQueueName(), new ConsumeOptions().exceptionHandler(connectionRecovery));

        var disposable = flux
                .subscribe(delivery -> Mono
                        .just(new String(delivery.getBody(), StandardCharsets.UTF_8))
                        .publishOn(Schedulers.boundedElastic())
                        .map(message -> createMessageFromRabbitDelivery(targetJavaType, delivery, message))
                        .doOnNext(consumer)
                        .retryWhen(retrySpec)
                        .onErrorComplete(x -> {
                            getAckDelivery(delivery).ifPresent(d -> d.nack(false));
                            return false;
                        })
                        .subscribe(message -> {
                            getAckDelivery(delivery).ifPresent(AcknowledgableDelivery::ack);
                        }));

        return disposable::dispose;
    }

    private Optional<AcknowledgableDelivery> getAckDelivery(Delivery delivery) {
        return Optional
                .of(delivery)
                .filter(d -> d instanceof AcknowledgableDelivery)
                .map(d -> (AcknowledgableDelivery) d);
    }

    private Map<String, Object> deadLetterConfig(String routingKey) {
        return Map.of(
                "x-dead-letter-exchange", DEAD_LETTER_EXCHANGE,
                "x-dead-letter-routing-key", routingKey);
    }


    private Message<Object> createMessageFromRabbitDelivery(JavaType targetJavaType, Delivery delivery, String message) {
        return MessageBuilder
                .withPayload(reflectionUtilsComponent.getParsedValueFromType(message, targetJavaType))
                .copyHeaders(delivery.getProperties().getHeaders())
                .build();
    }

    private Message<Object> createMessageFromKafkaRecord(
            JavaType targetJavaType,
            ReceiverRecord<Integer, String> record,
            String message) {
        return MessageBuilder
                .withPayload(reflectionUtilsComponent.getParsedValueFromType(message, targetJavaType))
                .copyHeaders(Arrays
                        .stream(Optional
                                .ofNullable(record.headers())
                                .map(Headers::toArray)
                                .orElse(new Header[0]))
                        .collect(Collectors.toMap(Header::key, Header::value))
                )
                .build();
    }
}
