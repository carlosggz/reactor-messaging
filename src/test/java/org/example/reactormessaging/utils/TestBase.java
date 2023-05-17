package org.example.reactormessaging.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@Import({ConsumersConfig.class})
@EmbeddedKafka(
        topics = {"events", "dto", "errorTopic"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        partitions = 2)
@Slf4j
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
public abstract class TestBase {
}
