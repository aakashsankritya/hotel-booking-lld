package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * A half-open stay range {@code [checkIn, checkOut)}. The guest sleeps on each date in the range,
 * and does not sleep on {@code checkOut}.
 *
 * <p>Maximum stay is {@value #MAX_NIGHTS} nights. This cap is a project assumption.
 * "Check-in must not be in the past" needs a {@code Clock}, so the booking service checks it
 * (use {@link #startsBefore(LocalDate)}).
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    public DateRange {
        Objects.requireNonNull(checkIn, "checkIn must not be null");
        Objects.requireNonNull(checkOut, "checkOut must not be null");
        if (!checkIn.isBefore(checkOut)) {
            throw new DomainValidationException("checkIn must be before checkOut: " + checkIn + " / " + checkOut);
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new DomainValidationException("stay must not be longer than " + MAX_NIGHTS + " nights");
        }
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Every night of the stay: checkIn inclusive, checkOut exclusive. */
    public List<LocalDate> dates() {
        return checkIn.datesUntil(checkOut).toList();
    }

    /** True if {@code date} is a night of the stay. */
    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !date.isBefore(checkIn) && date.isBefore(checkOut);
    }

    /** True if check-in is strictly before {@code date}. */
    public boolean startsBefore(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return checkIn.isBefore(date);
    }
}
