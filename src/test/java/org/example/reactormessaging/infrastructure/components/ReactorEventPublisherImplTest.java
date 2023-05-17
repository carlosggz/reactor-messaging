package org.example.reactormessaging.infrastructure.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reactormessaging.domain.exceptions.InvalidPayloadException;
import org.example.reactormessaging.domain.models.KafkaMessage;
import org.example.reactormessaging.domain.models.RabbitMessage;
import org.example.reactormessaging.utils.CustomDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactorEventPublisherImplTest {
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    ReactorEventPublisherImpl eventPublisher;


     @Test
    @SneakyThrows
    void invalidRabbitMessageThrowsInvalidPayloadException() {
        //given
        var message = RabbitMessage.builder()
                .payload(CustomDto.builder().build())
                .exchangeName("my-exchange")
                .build();
        when(objectMapper.writeValueAsString(message.getPayload())).thenThrow(JsonProcessingException.class);

        //when/then
        assertThrows(InvalidPayloadException.class, () -> eventPublisher.sendMessage(message).subscribe()) ;
    }

    @Test
    @SneakyThrows
    void invalidKafkaMessageThrowsInvalidPayloadException() {
        //given
        var message = KafkaMessage.builder()
                .payload(CustomDto.builder().build())
                .topic("my-topic")
                .build();
        when(objectMapper.writeValueAsString(message.getPayload())).thenThrow(JsonProcessingException.class);

        //when/then
        assertThrows(InvalidPayloadException.class, () -> eventPublisher.sendMessage(message).subscribe()) ;
    }

}
