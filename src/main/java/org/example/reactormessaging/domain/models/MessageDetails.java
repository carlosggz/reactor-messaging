package org.example.reactormessaging.domain.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@Data
@Builder
public class MessageDetails<T> implements Message<T> {
    private String routingKey;
    private T payload;
    private MessageHeaders headers;
}
