package com.rupeek.hotel.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.application.Fixture;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HoldExpiryTest {

    private static final LocalDate DEC_10 = LocalDate.of(2026, 12, 10);

    private final Fixture f = new Fixture(CancellationPolicyType.MODERATE, 1);

    @Test
    void expiredHoldBecomesExpiredAndFreesTheRoom() {
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
        f.clock.advance(Fixture.HOLD_TTL);

        int expired = f.bookingService.expireHolds();

        assertThat(expired).isEqualTo(1);
        assertThat(f.bookingService.get(booking.id()).status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(f.held(DEC_10)).isZero();
        f.bookingService.book(f.bookCommand(Fixture.stay(10, 12))); // the room can be booked again
    }

    @Test
    void holdThatIsStillValidIsLeftAlone() {
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
        f.clock.advance(Fixture.HOLD_TTL.minusSeconds(1));

        assertThat(f.bookingService.expireHolds()).isZero();
        assertThat(f.bookingService.get(booking.id()).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(f.held(DEC_10)).isEqualTo(1);
    }

    @Test
    void bookingPaidBeforeTheHoldEndedStaysConfirmed() {
        Booking booking = f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
        f.clock.advance(Fixture.HOLD_TTL.minusSeconds(1));
        f.paymentService.pay(booking.id(), Fixture.upi(), new IdempotencyKey("k1"));
        f.clock.advance(Duration.ofMinutes(5)); // now the old hold time has passed

        assertThat(f.bookingService.expireHolds()).isZero();
        assertThat(f.bookingService.get(booking.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(f.held(DEC_10)).isEqualTo(1);
    }

    @Test
    void sweepTwiceExpiresOnlyOnce() {
        f.bookingService.book(f.bookCommand(Fixture.stay(10, 12)));
        f.clock.advance(Fixture.HOLD_TTL);

        assertThat(f.bookingService.expireHolds()).isEqualTo(1);
        assertThat(f.bookingService.expireHolds()).isZero();
        assertThat(f.held(DEC_10)).isZero();
    }
}
