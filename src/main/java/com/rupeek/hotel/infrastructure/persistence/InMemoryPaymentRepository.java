package com.rupeek.hotel.infrastructure.persistence;

import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.repository.PaymentRepository;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.PaymentId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<PaymentId, Payment> store = new ConcurrentHashMap<>();
    private final Map<IdempotencyKey, Payment> byKey = new ConcurrentHashMap<>();

    @Override
    public void save(Payment payment) {
        if (!byKey.containsKey(payment.idempotencyKey())) {
            throw new IllegalStateException("use saveIfAbsent for a new payment");
        }
        store.put(payment.id(), payment);
    }

    @Override
    public Optional<Payment> saveIfAbsent(Payment payment) {
        Payment existing = byKey.putIfAbsent(payment.idempotencyKey(), payment); // atomic: one winner per key
        if (existing != null) {
            return Optional.of(existing);
        }
        store.put(payment.id(), payment);
        return Optional.empty();
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey key) {
        return Optional.ofNullable(byKey.get(key));
    }

    @Override
    public Optional<Payment> findSucceededByBookingId(BookingId bookingId) {
        return store.values().stream()
                .filter(payment -> payment.bookingId().equals(bookingId) && payment.isSuccessful())
                .findFirst();
    }
}
