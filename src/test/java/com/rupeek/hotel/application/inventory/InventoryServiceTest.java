package com.rupeek.hotel.application.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.rupeek.hotel.application.Fixture;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

    private static final LocalDate DEC_10 = LocalDate.of(2026, 12, 10);
    private static final LocalDate DEC_11 = LocalDate.of(2026, 12, 11);
    private static final LocalDate DEC_12 = LocalDate.of(2026, 12, 12);

    private final Fixture f = new Fixture(CancellationPolicyType.FLEXIBLE, 1);

    @Test
    void holdTakesOneUnitOnEveryNight() {
        f.inventory.hold(f.room, Fixture.stay(10, 13));

        assertThat(f.held(DEC_10)).isEqualTo(1);
        assertThat(f.held(DEC_11)).isEqualTo(1);
        assertThat(f.held(DEC_12)).isEqualTo(1);
        assertThat(f.held(LocalDate.of(2026, 12, 13))).isZero(); // check-out night stays free
    }

    @Test
    void oneFullNightInTheMiddleHoldsNothing() {
        f.inventory.hold(f.room, Fixture.stay(11, 12));

        assertThatExceptionOfType(InsufficientInventoryException.class)
                .isThrownBy(() -> f.inventory.hold(f.room, Fixture.stay(10, 13)));

        assertThat(f.held(DEC_10)).isZero();
        assertThat(f.held(DEC_11)).isEqualTo(1); // only the first hold
        assertThat(f.held(DEC_12)).isZero();
    }

    @Test
    void releaseGivesTheUnitsBack() {
        f.inventory.hold(f.room, Fixture.stay(10, 12));

        f.inventory.release(f.room.id(), Fixture.stay(10, 12));

        assertThat(f.held(DEC_10)).isZero();
        assertThat(f.held(DEC_11)).isZero();
        f.inventory.hold(f.room, Fixture.stay(10, 12)); // can be held again
    }

    @Test
    void releaseOfANightThatWasNeverHeldIsABug() {
        assertThatIllegalStateException().isThrownBy(() -> f.inventory.release(f.room.id(), Fixture.stay(10, 12)));
    }
}
