package org.example.reactormessaging.domain.exceptions;

public class RabbitExchangeException extends RuntimeException {

    public RabbitExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
