package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.value.InventoryKey;
import com.rupeek.hotel.domain.value.RoomTypeId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RoomInventoryTest {

    private static final LocalDate DATE = LocalDate.of(2027, 3, 10);
    private final InventoryKey key = new InventoryKey(RoomTypeId.newId(), DATE);
    private final RoomInventory inventory = RoomInventory.open(key, 5);

    @Test
    void openStartsWithEverythingAvailable() {
        assertThat(inventory.available()).isEqualTo(5);
        assertThat(inventory.heldUnits()).isZero();
        assertThat(inventory.key()).isEqualTo(key);
    }

    @Test
    void negativeTotalIsRejectedAndZeroIsAllowed() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> RoomInventory.open(key, -1));
        assertThat(RoomInventory.open(key, 0).available()).isZero();
    }

    @Test
    void holdUpToTotalIsOk() {
        inventory.hold(2);
        inventory.hold(3);

        assertThat(inventory.available()).isZero();
        assertThat(inventory.heldUnits()).isEqualTo(5);
    }

    @Test
    void holdMoreThanAvailableThrowsAndChangesNothing() {
        inventory.hold(2);

        assertThatExceptionOfType(InsufficientInventoryException.class)
                .isThrownBy(() -> inventory.hold(4))
                .satisfies(e -> {
                    assertThat(e.roomTypeId()).isEqualTo(key.roomTypeId());
                    assertThat(e.date()).isEqualTo(DATE);
                    assertThat(e.requested()).isEqualTo(4);
                    assertThat(e.available()).isEqualTo(3);
                });
        assertThat(inventory.available()).isEqualTo(3);
    }

    @Test
    void holdTotalPlusOneThrows() {
        assertThatExceptionOfType(InsufficientInventoryException.class).isThrownBy(() -> inventory.hold(6));
    }

    @Test
    void holdAndReleaseOfNonPositiveUnitsIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> inventory.hold(0));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> inventory.hold(-1));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> inventory.release(0));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> inventory.release(-1));
    }

    @Test
    void releaseMoreThanHeldThrowsIllegalState() {
        inventory.hold(2);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> inventory.release(3));
        assertThat(inventory.heldUnits()).isEqualTo(2);
    }

    @Test
    void holdThenReleaseRestoresAvailable() {
        int before = inventory.available();

        inventory.hold(3);
        inventory.release(3);

        assertThat(inventory.available()).isEqualTo(before);
    }

    @Test
    void canHold() {
        inventory.hold(3);

        assertThat(inventory.canHold(2)).isTrue();
        assertThat(inventory.canHold(3)).isFalse();
        assertThat(inventory.canHold(0)).isFalse();
        assertThat(inventory.canHold(-1)).isFalse();
    }

    @Test
    void equalityUsesKeyOnly() {
        RoomInventory sameKey = RoomInventory.open(key, 9);

        assertThat(inventory).isEqualTo(sameKey).hasSameHashCodeAs(sameKey);
        assertThat(inventory).isNotEqualTo(RoomInventory.open(new InventoryKey(key.roomTypeId(), DATE.plusDays(1)), 5));
    }
}
