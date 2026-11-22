package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Owner. Stops ids of different entities being mixed up. */
public record OwnerId(UUID value) {

    public OwnerId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OwnerId newId() {
        return new OwnerId(UUID.randomUUID());
    }

    /** Parses a path variable. Throws {@link DomainValidationException} for an invalid UUID. */
    public static OwnerId of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return new OwnerId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("invalid OwnerId: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
