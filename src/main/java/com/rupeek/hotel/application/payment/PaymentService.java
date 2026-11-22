package com.rupeek.hotel.application.payment;

import com.rupeek.hotel.application.common.KeyedLocks;
import com.rupeek.hotel.application.inventory.InventoryService;
import com.rupeek.hotel.domain.exception.BookingHoldExpiredException;
import com.rupeek.hotel.domain.exception.IdempotencyKeyConflictException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.payment.PaymentProcessorRegistry;
import com.rupeek.hotel.domain.repository.BookingRepository;
import com.rupeek.hotel.domain.repository.PaymentRepository;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.PaymentInstrument;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Pays for a held booking. Everything runs under the booking lock, in this order: replay check,
 * booking checks, charge. A request that fails a check never reaches the gateway.
 * A failed payment is final for its key. The guest must start a new booking, because
 * {@code PAYMENT_FAILED} is a terminal state.
 */
@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final InventoryService inventory;
    private final PaymentProcessorRegistry processors;
    private final KeyedLocks<BookingId> bookingLocks;
    private final Clock clock;

    public PaymentService(BookingRepository bookings, PaymentRepository payments, InventoryService inventory,
            PaymentProcessorRegistry processors, KeyedLocks<BookingId> bookingLocks, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.inventory = inventory;
        this.processors = processors;
        this.bookingLocks = bookingLocks;
        this.clock = clock;
    }

    public PaymentOutcome pay(BookingId bookingId, PaymentInstrument instrument, IdempotencyKey key) {
        Clock now = Clock.fixed(clock.instant(), clock.getZone()); // one instant: "valid at start" is "valid at confirm"
        return bookingLocks.withLock(bookingId, () -> payLocked(bookingId, instrument, key, now));
    }

    private PaymentOutcome payLocked(BookingId bookingId, PaymentInstrument instrument, IdempotencyKey key, Clock now) {
        Booking booking = bookings.getById(bookingId);

        Optional<Payment> existing = payments.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replay(existing.get(), booking);
        }
        if (booking.status() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidStateTransitionException("Booking", bookingId, booking.status(), BookingStatus.CONFIRMED);
        }
        if (booking.isHoldExpired(now.instant())) {
            throw new BookingHoldExpiredException(bookingId, booking.holdExpiresAt()); // the sweep frees the rooms
        }

        Payment payment = Payment.initiate(bookingId, booking.amount(), instrument, key, now);
        Optional<Payment> raced = payments.saveIfAbsent(payment);
        if (raced.isPresent()) {
            return replay(raced.get(), booking); // another booking took the key at the same moment
        }

        GatewayResult result;
        try {
            result = processors.forType(instrument.type()).charge(booking.amount(), instrument, key);
        } catch (RuntimeException e) {
            result = GatewayResult.failure("gateway error: " + e.getMessage()); // see README: a timeout is really "unknown"
        }
        if (result.success()) {
            payment.markSucceeded(result.reference());
            booking.confirm(now);
        } else {
            payment.markFailed(result.failureReason());
            booking.markPaymentFailed();
            inventory.release(booking.roomTypeId(), booking.stay());
        }
        payments.save(payment);
        bookings.save(booking);
        return new PaymentOutcome(payment, booking.status());
    }

    /** Same key and same booking: return the first result. Same key and another booking: reject. */
    private PaymentOutcome replay(Payment existing, Booking booking) {
        if (!existing.bookingId().equals(booking.id())) {
            throw new IdempotencyKeyConflictException(existing.idempotencyKey());
        }
        return new PaymentOutcome(existing, booking.status());
    }
}
