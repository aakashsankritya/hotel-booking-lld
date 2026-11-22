package com.rupeek.hotel.domain.exception;

/** Base class of every business-rule error. The web layer maps each subclass to one HTTP status. */
public abstract class DomainException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }
}
