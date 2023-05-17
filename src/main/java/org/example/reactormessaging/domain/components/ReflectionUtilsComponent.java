package org.example.reactormessaging.domain.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reactormessaging.domain.exceptions.InvalidParseException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ReflectionUtilsComponent {

    private final ApplicationContext context;
    private final ObjectMapper objectMapper;

    public Consumer<Message<?>> getConsumer(String consumerName) {
        Assert.isTrue(StringUtils.isNotBlank(consumerName), "Invalid consumer name");

        try {
            return (Consumer<Message<?>>)context.getBean(consumerName);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid consumer name: " + consumerName, ex);
        }
    }

    public Optional<JavaType> getJavaType(String canonicalName) {
        try {
            return Optional
                    .ofNullable(canonicalName)
                    .filter(StringUtils::isNotBlank)
                    .map(x -> objectMapper.getTypeFactory().constructFromCanonical(x));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid canonical name: " + canonicalName, ex);
        }
    }

    public Object getParsedValueFromType(String value, JavaType javaType) {

        try {
            return Objects.isNull(javaType) ? value : objectMapper.readValue(value, javaType) ;
        } catch (JsonProcessingException e) {
            throw new InvalidParseException("Error parsing received value", e);
        }
    }
}
