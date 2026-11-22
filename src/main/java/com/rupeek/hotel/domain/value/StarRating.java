package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;

/** Hotel star rating: 1 to 5. */
public record StarRating(int value) {

    public static final int MIN = 1;
    public static final int MAX = 5;

    public StarRating {
        if (value < MIN || value > MAX) {
            throw new DomainValidationException("star rating must be between " + MIN + " and " + MAX + ": " + value);
        }
    }

    public boolean isAtLeast(StarRating other) {
        Objects.requireNonNull(other, "other must not be null");
        return value >= other.value;
    }
}
