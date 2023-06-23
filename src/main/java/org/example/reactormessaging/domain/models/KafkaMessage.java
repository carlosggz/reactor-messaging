package org.example.reactormessaging.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class KafkaMessage {
    private String topic;
    private Object payload;

    @Builder.Default
    private String routingKey = null;

    @Builder.Default
    private Map<String, Object> headers = Map.of();
}
