package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;

/** Number of guests in one booking: 1 to {@value #MAX}. */
public record GuestCount(int value) {

    public static final int MIN = 1;
    public static final int MAX = 20;

    public GuestCount {
        if (value < MIN || value > MAX) {
            throw new DomainValidationException("guest count must be between " + MIN + " and " + MAX + ": " + value);
        }
    }

    public boolean fitsIn(int capacity) {
        return value <= capacity;
    }
}
