package com.rupeek.hotel.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.application.Fixture;
import com.rupeek.hotel.domain.exception.BookingHoldExpiredException;
import com.rupeek.hotel.domain.exception.IdempotencyKeyConflictException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.PaymentStatus;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.CardDetails;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.UpiDetails;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private static final LocalDate DEC_10 = LocalDate.of(2026, 12, 10);

    private final Fixture f = new Fixture(CancellationPolicyType.MODERATE, 2);

    private Booking held() {
        return f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
    }

    private static IdempotencyKey key(String value) {
        return new IdempotencyKey(value);
    }

    @Test
    void successConfirmsTheBooking() {
        Booking booking = held();

        PaymentOutcome outcome = f.paymentService.pay(booking.id(), Fixture.upi(), key("k1"));

        assertThat(outcome.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(outcome.payment().status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(outcome.payment().amount()).isEqualTo(booking.amount());
        assertThat(f.bookingService.get(booking.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(f.held(DEC_10)).isEqualTo(1); // a confirmed booking keeps its room
    }

    @Test
    void declinedPaymentFailsTheBookingAndFreesTheRoom() {
        Booking booking = held();

        PaymentOutcome outcome = f.paymentService.pay(booking.id(), Fixture.decliningCard(), key("k1"));

        assertThat(outcome.bookingStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(outcome.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outcome.payment().failureReason()).isEqualTo("declined");
        assertThat(f.held(DEC_10)).isZero();
    }

    @Test
    void expiredCardFailsWithoutCallingTheGateway() {
        Booking booking = held();
        CardDetails expired = new CardDetails("4242", YearMonth.of(2020, 1), "Ravi");

        PaymentOutcome outcome = f.paymentService.pay(booking.id(), expired, key("k1"));

        assertThat(outcome.bookingStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(f.gateway.chargeCount()).isZero();
    }

    @Test
    void sameKeyTwiceChargesOnceAndReturnsTheSamePayment() {
        Booking booking = held();

        PaymentOutcome first = f.paymentService.pay(booking.id(), Fixture.upi(), key("k1"));
        PaymentOutcome second = f.paymentService.pay(booking.id(), Fixture.upi(), key("k1"));

        assertThat(f.gateway.chargeCount()).isEqualTo(1);
        assertThat(second.payment()).isSameAs(first.payment());
        assertThat(second.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void replayOfADeclinedPaymentDoesNotChargeAgain() {
        Booking booking = held();
        f.paymentService.pay(booking.id(), Fixture.decliningCard(), key("k1"));

        PaymentOutcome replay = f.paymentService.pay(booking.id(), Fixture.decliningCard(), key("k1"));

        assertThat(f.gateway.chargeCount()).isEqualTo(1);
        assertThat(replay.bookingStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
    }

    @Test
    void sameKeyOnAnotherBookingIsRejected() {
        Booking first = held();
        Booking second = held();
        f.paymentService.pay(first.id(), Fixture.upi(), key("k1"));

        assertThatExceptionOfType(IdempotencyKeyConflictException.class)
                .isThrownBy(() -> f.paymentService.pay(second.id(), Fixture.upi(), key("k1")));
        assertThat(f.gateway.chargeCount()).isEqualTo(1);
        assertThat(f.bookingService.get(second.id()).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void payingAfterTheHoldEndedThrowsAndDoesNotCharge() {
        Booking booking = held();
        f.clock.advance(Fixture.HOLD_TTL.plusSeconds(1));

        assertThatExceptionOfType(BookingHoldExpiredException.class)
                .isThrownBy(() -> f.paymentService.pay(booking.id(), Fixture.upi(), key("k1")));
        assertThat(f.gateway.chargeCount()).isZero();
    }

    @Test
    void payingExactlyAtTheEndOfTheHoldIsTooLate() {
        Booking booking = held();
        f.clock.advance(Fixture.HOLD_TTL);

        assertThatExceptionOfType(BookingHoldExpiredException.class)
                .isThrownBy(() -> f.paymentService.pay(booking.id(), Fixture.upi(), key("k1")));
        assertThat(f.gateway.chargeCount()).isZero();
    }

    @Test
    void payingAConfirmedBookingWithANewKeyThrowsAndDoesNotCharge() {
        Booking booking = held();
        f.paymentService.pay(booking.id(), Fixture.upi(), key("k1"));

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> f.paymentService.pay(booking.id(), Fixture.upi(), key("k2")));
        assertThat(f.gateway.chargeCount()).isEqualTo(1);
    }

    @Test
    void payingAFailedBookingWithANewKeyThrowsAndDoesNotCharge() {
        Booking booking = held();
        f.paymentService.pay(booking.id(), Fixture.decliningCard(), key("k1"));

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> f.paymentService.pay(booking.id(), Fixture.upi(), key("k2")));
        assertThat(f.gateway.chargeCount()).isEqualTo(1);
    }

    @Test
    void twentyThreadsWithOneKeyChargeOnce() throws Exception {
        Booking booking = held();
        UpiDetails upi = Fixture.upi();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PaymentOutcome>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                results.add(executor.submit(() -> f.paymentService.pay(booking.id(), upi, key("same"))));
            }
            PaymentOutcome first = results.get(0).get();
            for (Future<PaymentOutcome> result : results) {
                assertThat(result.get().payment()).isSameAs(first.payment());
                assertThat(result.get().bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
            }
        }

        assertThat(f.gateway.chargeCount()).isEqualTo(1);
    }

    @Test
    void paymentRacingWithTheSweepStaysConfirmed() throws Exception {
        Booking booking = held();
        f.clock.advance(Fixture.HOLD_TTL.minus(Duration.ofSeconds(1))); // still valid at the start of both calls

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<PaymentOutcome> pay = executor.submit(() -> f.paymentService.pay(booking.id(), Fixture.upi(), key("k1")));
            Future<Integer> sweep = executor.submit(f.bookingService::expireHolds);
            pay.get();
            sweep.get();
        }

        assertThat(f.bookingService.get(booking.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(f.held(DEC_10)).isEqualTo(1);
    }

    @Test
    void gatewayErrorFailsThePaymentFreesTheRoomAndReplayReturnsTheFailedPayment() {
        Booking booking = held();
        f.gateway.throwOnCharge(new IllegalStateException("connection reset"));

        PaymentOutcome outcome = f.paymentService.pay(booking.id(), Fixture.upi(), key("k1"));

        assertThat(outcome.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outcome.payment().failureReason()).isEqualTo("gateway error: connection reset");
        assertThat(outcome.bookingStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(f.held(DEC_10)).isZero();

        PaymentOutcome replay = f.paymentService.pay(booking.id(), Fixture.upi(), key("k1"));

        assertThat(replay.payment()).isSameAs(outcome.payment());
        assertThat(f.gateway.chargeCount()).isEqualTo(1);
    }
}
