package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Property. Stops ids of different entities being mixed up. */
public record PropertyId(UUID value) {

    public PropertyId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static PropertyId newId() {
        return new PropertyId(UUID.randomUUID());
    }

    /** Parses a path variable. Throws {@link DomainValidationException} for an invalid UUID. */
    public static PropertyId of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return new PropertyId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("invalid PropertyId: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
