package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Booking. Stops ids of different entities being mixed up. */
public record BookingId(UUID value) {

    public BookingId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static BookingId newId() {
        return new BookingId(UUID.randomUUID());
    }

    /** Parses a path variable. Throws {@link DomainValidationException} for an invalid UUID. */
    public static BookingId of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return new BookingId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("invalid BookingId: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
