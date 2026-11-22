package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;

/** Client-supplied key that makes a booking or payment request safe to retry. */
public record IdempotencyKey(String value) {

    public static final int MAX_LENGTH = 64;

    public IdempotencyKey {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new DomainValidationException("idempotency key must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("idempotency key must not be longer than " + MAX_LENGTH + " characters");
        }
    }
}
