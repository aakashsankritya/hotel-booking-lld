package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;

/**
 * Price filter for search. A {@code null} bound is open. Both bounds are inclusive.
 */
public record PriceRange(Money min, Money max) {

    public PriceRange {
        if (min != null && max != null && min.isGreaterThan(max)) { // also rejects mixed currencies
            throw new DomainValidationException("min must not be greater than max: " + min + " / " + max);
        }
    }

    public boolean contains(Money price) {
        Objects.requireNonNull(price, "price must not be null");
        if (min != null && min.isGreaterThan(price)) {
            return false;
        }
        return max == null || !price.isGreaterThan(max);
    }
}
