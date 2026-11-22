package com.rupeek.hotel.domain.repository;

import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.value.BookingId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    void save(Booking booking);

    Optional<Booking> findById(BookingId id);

    /** Bookings that are still {@code PENDING_PAYMENT} and whose hold ended at or before {@code now}. */
    List<Booking> findExpiredHolds(Instant now);

    default Booking getById(BookingId id) {
        return findById(id).orElseThrow(() -> new EntityNotFoundException("Booking", id));
    }
}
