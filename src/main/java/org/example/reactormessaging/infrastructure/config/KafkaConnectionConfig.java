package org.example.reactormessaging.infrastructure.config;

import org.example.reactormessaging.infrastructure.properties.ReactorKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Configuration
public class KafkaConnectionConfig {
    @Bean
    public KafkaSender<String, String> kafkaSender(ReactorKafkaProperties reactorKafkaProperties) {
        return KafkaSender
                .create(SenderOptions.create(
                        Map.of(
                                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, reactorKafkaProperties.getServers(),
                                ProducerConfig.CLIENT_ID_CONFIG, reactorKafkaProperties.getClientId(),
                                ProducerConfig.ACKS_CONFIG, reactorKafkaProperties.getAcks(),
                                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class
                        )
                ));
    }

    @Bean
    public ReceiverOptions<Integer, String> receiverOptions(ReactorKafkaProperties reactorKafkaProperties) {
        return ReceiverOptions
                .create(
                        Map.of(
                                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, reactorKafkaProperties.getServers(),
                                ConsumerConfig.CLIENT_ID_CONFIG, reactorKafkaProperties.getClientId(),
                                ConsumerConfig.GROUP_ID_CONFIG, reactorKafkaProperties.getGroupId(),
                                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, reactorKafkaProperties.getOffset()
                        )
                );
    }
}
