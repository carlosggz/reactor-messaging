package org.example.reactormessaging.domain.exceptions;

public class InvalidPayloadException extends RuntimeException {
    public InvalidPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
