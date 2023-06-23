package org.example.reactormessaging.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class RabbitMessage {
    private String exchangeName;
    private Object payload;

    @Builder.Default
    private String routingKey = "#";

    @Builder.Default
    private Map<String, Object> headers = Map.of();
}
