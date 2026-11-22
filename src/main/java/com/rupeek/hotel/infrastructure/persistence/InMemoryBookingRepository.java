package com.rupeek.hotel.infrastructure.persistence;

import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.repository.BookingRepository;
import com.rupeek.hotel.domain.value.BookingId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<BookingId, Booking> store = new ConcurrentHashMap<>();

    @Override
    public void save(Booking booking) {
        store.put(booking.id(), booking);
    }

    @Override
    public Optional<Booking> findById(BookingId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Booking> findExpiredHolds(Instant now) {
        return store.values().stream()
                .filter(booking -> booking.status() == BookingStatus.PENDING_PAYMENT && booking.isHoldExpired(now))
                .toList();
    }
}
