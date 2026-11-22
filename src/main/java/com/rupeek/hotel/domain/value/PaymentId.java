package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Payment. Stops ids of different entities being mixed up. */
public record PaymentId(UUID value) {

    public PaymentId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static PaymentId newId() {
        return new PaymentId(UUID.randomUUID());
    }

    /** Parses a path variable. Throws {@link DomainValidationException} for an invalid UUID. */
    public static PaymentId of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return new PaymentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("invalid PaymentId: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
