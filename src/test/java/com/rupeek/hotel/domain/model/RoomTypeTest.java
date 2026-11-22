package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RoomTypeTest {

    private static final Money PRICE = Money.of("2500", "INR");

    @Test
    void createStoresTheFields() {
        RoomType room = RoomType.create(" Deluxe ", 3, 10, PRICE);

        assertThat(room.name()).isEqualTo("Deluxe");
        assertThat(room.capacity()).isEqualTo(3);
        assertThat(room.totalUnits()).isEqualTo(10);
        assertThat(room.basePricePerNight()).isEqualTo(PRICE);
    }

    @Test
    void blankNameIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> RoomType.create(" ", 2, 1, PRICE));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void capacityOutsideOneToTenIsRejected(int capacity) {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> RoomType.create("Deluxe", capacity, 1, PRICE));
    }

    @Test
    void capacityBoundariesAreAccepted() {
        assertThat(RoomType.create("A", 1, 1, PRICE).capacity()).isEqualTo(1);
        assertThat(RoomType.create("B", 10, 1, PRICE).capacity()).isEqualTo(10);
    }

    @Test
    void totalUnitsMustBeAtLeastOne() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> RoomType.create("Deluxe", 2, 0, PRICE));
    }

    @Test
    void basePriceMustBeGreaterThanZero() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> RoomType.create("Deluxe", 2, 1, Money.of("0", "INR")));
    }

    @Test
    void canHost() {
        RoomType room = RoomType.create("Deluxe", 3, 1, PRICE);

        assertThat(room.canHost(new GuestCount(3))).isTrue();
        assertThat(room.canHost(new GuestCount(4))).isFalse();
    }

    @Test
    void equalityUsesIdOnly() {
        RoomType a = RoomType.create("Deluxe", 3, 1, PRICE);
        RoomType b = RoomType.create("Deluxe", 3, 1, PRICE);

        assertThat(a).isEqualTo(a).isNotEqualTo(b);
    }
}
