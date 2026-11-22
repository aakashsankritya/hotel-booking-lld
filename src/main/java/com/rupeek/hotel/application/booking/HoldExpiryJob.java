package com.rupeek.hotel.application.booking;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the hold sweep on a timer. The logic is in {@link BookingService#expireHolds()}. */
@Component
public class HoldExpiryJob {

    private final BookingService bookings;

    public HoldExpiryJob(BookingService bookings) {
        this.bookings = bookings;
    }

    @Scheduled(fixedDelayString = "${booking.sweep-interval}")
    public void sweep() {
        bookings.expireHolds();
    }
}
