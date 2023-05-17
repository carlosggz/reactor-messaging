package org.example.reactormessaging.domain.exceptions;

public class RetryExhaustedException extends RuntimeException {
    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
