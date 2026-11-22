package com.rupeek.hotel.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.application.cancellation.CancellationOutcome;
import com.rupeek.hotel.application.payment.PaymentOutcome;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.PaymentStatus;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import java.time.LocalDate;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Pay and cancel one booking at the same moment. Both take the booking lock, so they run one after the other.
 * Either order is correct: pay then cancel (the guest is charged and refunded), or cancel then pay (the payment
 * is rejected before the gateway is called). The result must never mix the two.
 */
class PayCancelRaceTest {

    private static final int ROUNDS = 200;

    @Test
    void payAndCancelAtTheSameTimeLeaveOneConsistentResult() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            Fixture f = new Fixture(CancellationPolicyType.MODERATE, 2);
            f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))); // another guest holds one of two units
            Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
            IdempotencyKey key = new IdempotencyKey("race-" + round);
            CyclicBarrier start = new CyclicBarrier(2);

            PaymentOutcome paid;
            CancellationOutcome cancelled;
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<PaymentOutcome> pay = executor.submit(() -> {
                    start.await();
                    try {
                        return f.paymentService.pay(booking.id(), Fixture.upi(), key);
                    } catch (InvalidStateTransitionException e) {
                        return null; // cancel came first
                    }
                });
                Future<CancellationOutcome> cancel = executor.submit(() -> {
                    start.await();
                    return f.cancellationService.cancel(booking.id());
                });
                paid = pay.get();
                cancelled = cancel.get();
            }

            assertThat(f.bookings.getById(booking.id()).status()).isEqualTo(BookingStatus.CANCELLED);
            // the other guest's unit is still held, so the cancelled unit was released exactly once
            assertThat(f.held(LocalDate.of(2026, 12, 10))).isEqualTo(1);
            assertThat(f.held(LocalDate.of(2026, 12, 11))).isEqualTo(1);
            if (paid == null) {
                assertThat(f.gateway.chargeCount()).isZero();
                assertThat(cancelled.quote()).isNull(); // nothing was paid
                assertThat(cancelled.refund()).isNull();
            } else {
                assertThat(f.gateway.chargeCount()).isEqualTo(1);
                assertThat(paid.payment().status()).isEqualTo(PaymentStatus.REFUNDED);
                assertThat(cancelled.quote().refundAmount()).isEqualTo(Money.of("6000", "INR"));
                assertThat(cancelled.refund().success()).isTrue();
            }
        }
    }
}
