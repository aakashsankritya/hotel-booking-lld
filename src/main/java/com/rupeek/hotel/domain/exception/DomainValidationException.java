package com.rupeek.hotel.domain.exception;

/** An input or a rule check failed. HTTP 400. */
public class DomainValidationException extends DomainException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public DomainValidationException(String message) {
        super(message);
    }
}
