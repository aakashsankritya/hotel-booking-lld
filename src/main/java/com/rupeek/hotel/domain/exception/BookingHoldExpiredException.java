package com.rupeek.hotel.domain.exception;

import com.rupeek.hotel.domain.value.BookingId;
import java.time.Instant;

/** The booking hold ended before the payment finished. HTTP 409. */
public class BookingHoldExpiredException extends DomainException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final transient BookingId bookingId;
    private final Instant expiredAt;

    public BookingHoldExpiredException(BookingId bookingId, Instant expiredAt) {
        super("Hold on booking " + bookingId + " expired at " + expiredAt);
        this.bookingId = bookingId;
        this.expiredAt = expiredAt;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public Instant expiredAt() {
        return expiredAt;
    }
}
