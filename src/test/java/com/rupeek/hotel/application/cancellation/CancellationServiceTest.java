package com.rupeek.hotel.application.cancellation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.application.Fixture;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.search.PropertySearchResult;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CancellationServiceTest {

    private static final LocalDate DEC_10 = LocalDate.of(2026, 12, 10);

    private static SearchCriteria goa() {
        return new SearchCriteria("Goa", null, Fixture.stay(10, 12), new GuestCount(2), null, null, null);
    }

    private static Booking pay(Fixture f, Booking booking) {
        f.paymentService.pay(booking.id(), Fixture.upi(), new IdempotencyKey("k-" + booking.id().value()));
        return booking;
    }

    @Test
    void confirmedModerateBookingFourDaysOutGetsAFullRefundAndTheRoomIsSearchableAgain() {
        Fixture f = new Fixture(CancellationPolicyType.MODERATE, 1);
        Booking booking = pay(f, f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))));
        assertThat(f.search.search(goa())).isEmpty(); // sold out while booked
        f.clock.advance(Duration.ofDays(5)); // now Dec 6 06:00: check-in is 4 days and 6 hours away

        CancellationOutcome outcome = f.cancellationService.cancel(booking.id());

        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(outcome.quote().refundAmount()).isEqualTo(Money.of("6000", "INR"));
        assertThat(outcome.refund().success()).isTrue();
        assertThat(outcome.refund().reference()).isNotBlank();
        assertThat(f.held(DEC_10)).isZero();
        List<PropertySearchResult> found = f.search.search(goa());
        assertThat(found).hasSize(1);
    }

    @Test
    void moderateBookingTwoDaysOutGetsHalf() {
        Fixture f = new Fixture(CancellationPolicyType.MODERATE, 1);
        Booking booking = pay(f, f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))));
        f.clock.advance(Duration.ofDays(7)); // Dec 8 06:00: 54 hours before check-in

        CancellationOutcome outcome = f.cancellationService.cancel(booking.id());

        assertThat(outcome.quote().refundAmount()).isEqualTo(Money.of("3000", "INR"));
        assertThat(outcome.quote().cancellationFee()).isEqualTo(Money.of("3000", "INR"));
        assertThat(outcome.refund().success()).isTrue();
    }

    @Test
    void flexibleBookingWithinADayGetsNothingAndNoRefundIsCreated() {
        Fixture f = new Fixture(CancellationPolicyType.FLEXIBLE, 1);
        Booking booking = pay(f, f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))));
        f.clock.advance(Duration.ofDays(8).plusHours(12)); // Dec 9 18:00: 18 hours before check-in

        CancellationOutcome outcome = f.cancellationService.cancel(booking.id());

        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(outcome.quote().refundAmount().isZero()).isTrue();
        assertThat(outcome.refund()).isNull();
        assertThat(f.held(DEC_10)).isZero(); // the room is free even without a refund
    }

    @Test
    void nonRefundableBookingGetsNothing() {
        Fixture f = new Fixture(CancellationPolicyType.NON_REFUNDABLE, 1);
        Booking booking = pay(f, f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))));

        CancellationOutcome outcome = f.cancellationService.cancel(booking.id());

        assertThat(outcome.quote().refundAmount().isZero()).isTrue();
        assertThat(outcome.refund()).isNull();
    }

    @Test
    void pendingBookingIsCancelledWithoutAQuoteAndTheRoomIsFree() {
        Fixture f = new Fixture(CancellationPolicyType.MODERATE, 1);
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));

        CancellationOutcome outcome = f.cancellationService.cancel(booking.id());

        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(outcome.quote()).isNull();
        assertThat(outcome.refund()).isNull();
        assertThat(f.held(DEC_10)).isZero();
    }

    @Test
    void cancellingTwiceThrowsAndReleasesOnlyOnce() {
        Fixture f = new Fixture(CancellationPolicyType.MODERATE, 2);
        Booking other = f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
        f.cancellationService.cancel(booking.id());

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> f.cancellationService.cancel(booking.id()));
        assertThat(f.held(DEC_10)).isEqualTo(1); // the other booking still holds its unit
        assertThat(other.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void gatewayErrorOnRefundIsReportedAndTheBookingStaysCancelled() {
        Fixture f = new Fixture(CancellationPolicyType.MODERATE, 1);
        Booking booking = pay(f, f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))));
        f.clock.advance(Duration.ofDays(5));
        f.gateway.throwOnRefund(new IllegalStateException("connection reset"));

        CancellationOutcome outcome = f.cancellationService.cancel(booking.id());

        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(outcome.refund().success()).isFalse();
        assertThat(outcome.refund().failureReason()).isEqualTo("gateway error: connection reset");
        assertThat(f.payments.findSucceededByBookingId(booking.id())).isPresent(); // captured, not refunded
        assertThat(f.held(DEC_10)).isZero();
    }

    @Test
    void cancellingOnTheCheckInDayIsRejected() {
        Fixture f = new Fixture(CancellationPolicyType.FLEXIBLE, 1);
        Booking booking = pay(f, f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))));
        f.clock.advance(Duration.ofDays(9).plusHours(1)); // Dec 10 07:00

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> f.cancellationService.cancel(booking.id()));
        assertThat(f.held(DEC_10)).isEqualTo(1);
    }
}
