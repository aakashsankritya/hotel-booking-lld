package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.BookingHoldExpiredException;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.GuestDetails;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PropertyId;
import com.rupeek.hotel.domain.value.RoomTypeId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookingTest {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Instant T0 = Instant.parse("2027-03-01T10:00:00Z");
    private static final LocalDate CHECK_IN = LocalDate.of(2027, 3, 10);
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    private static Clock after(Duration duration) {
        return Clock.offset(CLOCK, duration);
    }

    private static Clock at(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static Booking newBooking() {
        return Booking.hold(PropertyId.newId(), RoomTypeId.newId(),
                new GuestDetails("Asha Rao", "asha@example.com"),
                new DateRange(CHECK_IN, CHECK_IN.plusDays(2)), new GuestCount(2), Money.of("6000", "INR"),
                CLOCK, TTL);
    }

    @Test
    void holdCreatesAPendingBookingWithAHoldDeadline() {
        Booking booking = newBooking();

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.createdAt()).isEqualTo(T0);
        assertThat(booking.updatedAt()).isEqualTo(T0);
        assertThat(booking.holdExpiresAt()).isEqualTo(T0.plus(TTL));
        assertThat(booking.cancelledAt()).isNull();
        assertThat(booking.version()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void holdRejectsAnEmptyOrNegativeTtl(long minutes) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Booking.hold(
                PropertyId.newId(), RoomTypeId.newId(), new GuestDetails("Asha", "a@b.com"),
                new DateRange(CHECK_IN, CHECK_IN.plusDays(1)), new GuestCount(1), Money.of("1", "INR"), CLOCK,
                Duration.ofMinutes(minutes)));
    }

    @Test
    void fullHappyPathHoldThenConfirmThenCancel() {
        Booking booking = newBooking();

        booking.confirm(after(Duration.ofMinutes(5)));
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.updatedAt()).isEqualTo(T0.plus(Duration.ofMinutes(5)));

        BookingStatus previous = booking.cancel(after(Duration.ofHours(1)));
        assertThat(previous).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.cancelledAt()).isEqualTo(T0.plus(Duration.ofHours(1)));
    }

    @Test
    void confirmAfterTheTtlThrowsAndChangesNothing() {
        Booking booking = newBooking();

        assertThatExceptionOfType(BookingHoldExpiredException.class)
                .isThrownBy(() -> booking.confirm(after(TTL.plusSeconds(1))))
                .satisfies(e -> {
                    assertThat(e.bookingId()).isEqualTo(booking.id());
                    assertThat(e.expiredAt()).isEqualTo(T0.plus(TTL));
                });
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.version()).isZero();
    }

    @Test
    void confirmExactlyAtTheDeadlineThrows() {
        Booking booking = newBooking();

        assertThatExceptionOfType(BookingHoldExpiredException.class).isThrownBy(() -> booking.confirm(after(TTL)));
    }

    @Test
    void confirmJustBeforeTheDeadlineWorks() {
        Booking booking = newBooking();

        booking.confirm(after(TTL.minusMillis(1)));

        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirmingAConfirmedBookingIsAnInvalidTransitionEvenAfterTheTtl() {
        Booking booking = newBooking();
        booking.confirm(after(Duration.ofMinutes(1)));

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> booking.confirm(after(Duration.ofHours(5))))
                .satisfies(e -> {
                    assertThat(e.from()).isEqualTo(BookingStatus.CONFIRMED);
                    assertThat(e.to()).isEqualTo(BookingStatus.CONFIRMED);
                });
    }

    @Test
    void cancelOnAConfirmedBookingReturnsConfirmed() {
        Booking booking = newBooking();
        booking.confirm(CLOCK);

        assertThat(booking.cancel(CLOCK)).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancelOnAPendingBookingReturnsPendingPayment() {
        Booking booking = newBooking();

        assertThat(booking.cancel(CLOCK)).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void aSecondCancelThrows() {
        Booking booking = newBooking();
        booking.cancel(CLOCK);

        assertThatExceptionOfType(InvalidStateTransitionException.class).isThrownBy(() -> booking.cancel(CLOCK));
        assertThat(booking.version()).isEqualTo(1);
    }

    @Test
    void cancelTheDayBeforeCheckInWorks() {
        Booking booking = newBooking();

        booking.cancel(at("2027-03-09T23:59:59Z"));

        assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelOnTheCheckInDateThrows() {
        Booking booking = newBooking();

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> booking.cancel(at("2027-03-10T00:00:00Z")));
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.cancelledAt()).isNull();
    }

    @Test
    void cancelAfterTheCheckInDateThrows() {
        Booking booking = newBooking();
        booking.confirm(CLOCK);

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> booking.cancel(at("2027-03-12T09:00:00Z")));
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void theClockZoneDecidesWhatTodayIs() {
        Booking booking = newBooking();
        // 2027-03-09T20:00Z is already 2027-03-10 in India (UTC+5:30): check-in day, so no cancel
        Clock india = Clock.fixed(Instant.parse("2027-03-09T20:00:00Z"), ZoneOffset.ofHoursMinutes(5, 30));

        assertThatExceptionOfType(InvalidStateTransitionException.class).isThrownBy(() -> booking.cancel(india));
    }

    @Test
    void versionIncreasesByOneForEachTransition() {
        Booking booking = newBooking();
        assertThat(booking.version()).isZero();

        booking.confirm(CLOCK);
        assertThat(booking.version()).isEqualTo(1);

        booking.cancel(CLOCK);
        assertThat(booking.version()).isEqualTo(2);
    }

    @Test
    void markPaymentFailedMovesPendingToPaymentFailedAndThenItIsTerminal() {
        Booking booking = newBooking();

        booking.markPaymentFailed(CLOCK);

        assertThat(booking.status()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(booking.version()).isEqualTo(1);
        assertThatExceptionOfType(InvalidStateTransitionException.class).isThrownBy(() -> booking.confirm(CLOCK));
        assertThatExceptionOfType(InvalidStateTransitionException.class).isThrownBy(() -> booking.cancel(CLOCK));
    }

    @Test
    void expireBeforeTheDeadlineThrows() {
        Booking booking = newBooking();

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> booking.expire(after(TTL.minusSeconds(1))));
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.version()).isZero();
    }

    @Test
    void expireAtOrAfterTheDeadlineWorks() {
        Booking atDeadline = newBooking();
        atDeadline.expire(after(TTL));
        Booking afterDeadline = newBooking();
        afterDeadline.expire(after(TTL.plusHours(1)));

        assertThat(atDeadline.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(afterDeadline.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(atDeadline.version()).isEqualTo(1);
    }

    @Test
    void expireOnAConfirmedBookingThrows() {
        Booking booking = newBooking();
        booking.confirm(CLOCK);

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> booking.expire(after(TTL.plusHours(1))));
    }

    @Test
    void isHoldExpiredUsesGreaterOrEqual() {
        Booking booking = newBooking();

        assertThat(booking.isHoldExpired(T0.plus(TTL).minusMillis(1))).isFalse();
        assertThat(booking.isHoldExpired(T0.plus(TTL))).isTrue();
        assertThat(booking.isHoldExpired(T0.plus(TTL).plusMillis(1))).isTrue();
    }

    @Test
    void equalityUsesIdOnly() {
        Booking a = newBooking();
        Booking b = newBooking();

        assertThat(a).isEqualTo(a).isNotEqualTo(b);
    }
}
