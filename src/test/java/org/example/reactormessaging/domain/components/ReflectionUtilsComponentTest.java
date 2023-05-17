package org.example.reactormessaging.domain.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reactormessaging.domain.exceptions.InvalidParseException;
import org.example.reactormessaging.utils.CustomDto;
import org.example.reactormessaging.utils.TestBase;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;


class ReflectionUtilsComponentTest extends TestBase {

    @Autowired
    ReflectionUtilsComponent reflectionUtilsComponent;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void getValidConsumerReturnsTheValue() {
        Assertions.assertDoesNotThrow(() -> reflectionUtilsComponent.getConsumer("fromRouted"));
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void invalidConsumerNameThrowsException(String name) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> reflectionUtilsComponent.getConsumer(name));
    }

    @Test
    void unknownConsumerNameThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> reflectionUtilsComponent.getConsumer("xxxx"));
    }

    @Test
    void getJavaTypeByCanonicalNameReturnsValidType() {
        var javaType = reflectionUtilsComponent
                .getJavaType("org.example.reactormessaging.utils.CustomDto");

        Assertions.assertTrue(javaType.isPresent());
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void invalidCanonicalNameReturnsEmptyResult(String name) {
        Assertions.assertTrue(reflectionUtilsComponent.getJavaType(name).isEmpty());
    }

    @Test
    void whenUnknownCanonicalNameAnIllegalArgumentExceptionIsRaised() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> reflectionUtilsComponent
                .getJavaType("xxx"));
    }

    @Test
    void parseValueFromNullTypeReturnsTheString() {
        val value = "test";
        Assertions.assertEquals(value, reflectionUtilsComponent.getParsedValueFromType(value, null));
    }

    @Test
    @SneakyThrows
    void parseValueFromJavaTypeTypeReturnsTheObject() {
        //given
        val value = CustomDto.builder()
                .id(123)
                .name("some name")
                .build();
        val javaType = reflectionUtilsComponent
                .getJavaType("org.example.reactormessaging.utils.CustomDto")
                .orElseThrow();

        //when
        val result = reflectionUtilsComponent
                .getParsedValueFromType(objectMapper.writeValueAsString(value), javaType);

        //then
        Assertions.assertEquals(value, result);
    }

    @Test
    @SneakyThrows
    void parseInvalidValueThrowsException() {
        //given
        val javaType = reflectionUtilsComponent
                .getJavaType("org.example.reactormessaging.utils.CustomDto")
                .orElseThrow();

        //when/then
        Assertions.assertThrows(InvalidParseException.class, () -> reflectionUtilsComponent
                .getParsedValueFromType("invalid value", javaType));
    }

    static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of((String)null),
                Arguments.of(""),
                Arguments.of("   ")
        );
    }
}
