package com.tiddev.quarkus.mediator;

/**
 * Signals mediator discovery or dispatch errors.
 */
public class MediatorException extends RuntimeException {
    /**
     * Creates a new mediator exception.
     *
     * @param message the error message
     */
    public MediatorException(String message) {
        super(message);
    }

    /**
     * Creates a new mediator exception with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public MediatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
