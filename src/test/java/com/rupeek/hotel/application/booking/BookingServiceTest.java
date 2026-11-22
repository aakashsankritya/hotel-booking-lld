package com.rupeek.hotel.application.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.application.Fixture;
import com.rupeek.hotel.domain.exception.CapacityExceededException;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.GuestDetails;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RoomTypeId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

    private final Fixture f = new Fixture(CancellationPolicyType.MODERATE, 1);

    @Test
    void newBookingWaitsForPaymentAndHoldsTheRoom() {
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 13)));

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.holdExpiresAt()).isEqualTo(f.clock.instant().plus(Fixture.HOLD_TTL));
        assertThat(f.held(LocalDate.of(2026, 12, 10))).isEqualTo(1);
        assertThat(f.bookingService.get(booking.id())).isSameAs(booking);
    }

    @Test
    void amountIsNightsTimesBasePrice() {
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 13)));

        assertThat(booking.amount()).isEqualTo(Money.of("9000", "INR"));
    }

    @Test
    void checkInInThePastThrows() {
        DateRange past = new DateRange(LocalDate.of(2026, 11, 28), LocalDate.of(2026, 11, 30));

        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> f.bookingService.book(f.bookCommand(past)));
        assertThat(f.held(LocalDate.of(2026, 11, 28))).isZero();
    }

    @Test
    void checkInTodayIsAllowed() {
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(1, 2)));

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void tooManyGuestsThrowsAndHoldsNothing() {
        BookCommand command = new BookCommand(f.property.id(), f.room.id(),
                new GuestDetails("Ravi", "ravi@example.com"), Fixture.stay(10, 12), new GuestCount(3));

        assertThatExceptionOfType(CapacityExceededException.class).isThrownBy(() -> f.bookingService.book(command));
        assertThat(f.held(LocalDate.of(2026, 12, 10))).isZero();
    }

    @Test
    void unknownRoomTypeThrowsNotFound() {
        BookCommand command = new BookCommand(f.property.id(),
                RoomTypeId.newId(), new GuestDetails("Ravi", "ravi@example.com"),
                Fixture.stay(10, 12), new GuestCount(2));

        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> f.bookingService.book(command));
    }

    @Test
    void unknownBookingThrowsNotFound() {
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> f.bookingService.get(BookingId.newId()));
    }

    @Test
    void withOneUnitOverlappingStaysFailAndBackToBackStaysWork() {
        f.bookingService.book(f.bookCommand(Fixture.stay(10, 14))); // nights 10,11,12,13

        f.bookingService.book(f.bookCommand(Fixture.stay(14, 17))); // starts on the check-out day: fine
        assertThatExceptionOfType(InsufficientInventoryException.class)
                .isThrownBy(() -> f.bookingService.book(f.bookCommand(Fixture.stay(13, 15))));
    }
}
