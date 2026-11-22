package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a RoomType. Stops ids of different entities being mixed up. */
public record RoomTypeId(UUID value) {

    public RoomTypeId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static RoomTypeId newId() {
        return new RoomTypeId(UUID.randomUUID());
    }

    /** Parses a path variable. Throws {@link DomainValidationException} for an invalid UUID. */
    public static RoomTypeId of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return new RoomTypeId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("invalid RoomTypeId: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
