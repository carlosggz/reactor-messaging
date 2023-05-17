package org.example.reactormessaging.domain.exceptions;

public class InvalidParseException extends RuntimeException {
    public InvalidParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
