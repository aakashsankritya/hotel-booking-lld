package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Refund. Stops ids of different entities being mixed up. */
public record RefundId(UUID value) {

    public RefundId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static RefundId newId() {
        return new RefundId(UUID.randomUUID());
    }

    /** Parses a path variable. Throws {@link DomainValidationException} for an invalid UUID. */
    public static RefundId of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return new RefundId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("invalid RefundId: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
