package com.rupeek.hotel.domain.exception;

import com.rupeek.hotel.domain.value.IdempotencyKey;

/** The idempotency key was already used for a payment of another booking. HTTP 422. */
public class IdempotencyKeyConflictException extends DomainException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final transient IdempotencyKey key;

    public IdempotencyKeyConflictException(IdempotencyKey key) {
        super("Idempotency key " + key.value() + " was already used for another booking");
        this.key = key;
    }

    public IdempotencyKey key() {
        return key;
    }
}
