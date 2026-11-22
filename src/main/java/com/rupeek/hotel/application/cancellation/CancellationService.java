package com.rupeek.hotel.application.cancellation;

import com.rupeek.hotel.application.common.KeyedLocks;
import com.rupeek.hotel.application.inventory.InventoryService;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.payment.PaymentProcessorRegistry;
import com.rupeek.hotel.domain.policy.CancellationPolicyResolver;
import com.rupeek.hotel.domain.repository.BookingRepository;
import com.rupeek.hotel.domain.repository.PaymentRepository;
import com.rupeek.hotel.domain.repository.PropertyRepository;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RefundQuote;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Cancels a booking, frees its rooms and refunds by the policy of the property.
 * If the gateway fails the refund, the booking stays cancelled and the outcome carries the failure.
 */
@Service
public class CancellationService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final PropertyRepository properties;
    private final InventoryService inventory;
    private final CancellationPolicyResolver policies;
    private final PaymentProcessorRegistry processors;
    private final KeyedLocks<BookingId> bookingLocks;
    private final Clock clock;

    public CancellationService(BookingRepository bookings, PaymentRepository payments, PropertyRepository properties,
            InventoryService inventory, CancellationPolicyResolver policies, PaymentProcessorRegistry processors,
            KeyedLocks<BookingId> bookingLocks, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.properties = properties;
        this.inventory = inventory;
        this.policies = policies;
        this.processors = processors;
        this.bookingLocks = bookingLocks;
        this.clock = clock;
    }

    public CancellationOutcome cancel(BookingId bookingId) {
        Clock now = Clock.fixed(clock.instant(), clock.getZone());
        return bookingLocks.withLock(bookingId, () -> cancelLocked(bookingId, now));
    }

    private CancellationOutcome cancelLocked(BookingId bookingId, Clock now) {
        Booking booking = bookings.getById(bookingId);
        BookingStatus previous = booking.cancel(now);
        inventory.release(booking.roomTypeId(), booking.stay());
        bookings.save(booking);
        if (previous != BookingStatus.CONFIRMED) {
            return new CancellationOutcome(booking, null, null); // nothing was paid
        }

        Payment payment = payments.findSucceededByBookingId(bookingId).orElseThrow(() ->
                new IllegalStateException("Confirmed booking has no succeeded payment: " + bookingId));
        Property property = properties.getById(booking.propertyId());
        RefundQuote quote = policies.resolve(property.cancellationPolicyType())
                .quote(payment.amount(), booking.stay(), now.instant());
        if (quote.refundAmount().isZero()) {
            return new CancellationOutcome(booking, quote, null);
        }

        GatewayResult refund = refund(payment, quote.refundAmount());
        if (refund.success()) {
            payment.recordRefund(quote.refundAmount());
            payments.save(payment);
        }
        return new CancellationOutcome(booking, quote, refund);
    }

    /** A gateway exception counts as a failed refund. The booking stays cancelled either way. */
    private GatewayResult refund(Payment payment, Money amount) {
        try {
            return processors.forType(payment.method()).refund(payment.gatewayReference(), amount);
        } catch (RuntimeException e) {
            return GatewayResult.failure("gateway error: " + e.getMessage());
        }
    }
}
