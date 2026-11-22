package com.rupeek.hotel.domain.repository;

import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import java.util.Optional;

public interface PaymentRepository {

    /**
     * Saves a payment that {@link #saveIfAbsent} already stored. Use it after a status change.
     *
     * @throws IllegalStateException if the payment is new
     */
    void save(Payment payment);

    /**
     * Saves a new payment only if no payment has its idempotency key. This call is atomic.
     *
     * @return empty if the payment was saved; the payment that already holds the key otherwise.
     *         The caller must check that the existing payment is for the same booking.
     */
    Optional<Payment> saveIfAbsent(Payment payment);

    Optional<Payment> findByIdempotencyKey(IdempotencyKey key);

    /** The payment of the booking that took the money, if there is one. */
    Optional<Payment> findSucceededByBookingId(BookingId bookingId);
}
