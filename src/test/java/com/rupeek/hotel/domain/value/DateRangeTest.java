package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    private static LocalDate jan(int day) {
        return LocalDate.of(2027, 1, day);
    }

    private static DateRange range(int checkIn, int checkOut) {
        return new DateRange(jan(checkIn), jan(checkOut));
    }

    @Test
    void checkOutOnOrBeforeCheckInIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> range(5, 5));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> range(5, 4));
    }

    @Test
    void nullDatesAreRejected() {
        assertThatNullPointerException().isThrownBy(() -> new DateRange(null, jan(2)));
        assertThatNullPointerException().isThrownBy(() -> new DateRange(jan(1), null));
    }

    @Test
    void maxThirtyNights() {
        LocalDate in = jan(1);

        assertThat(new DateRange(in, in.plusDays(DateRange.MAX_NIGHTS)).nights()).isEqualTo(30);
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> new DateRange(in, in.plusDays(DateRange.MAX_NIGHTS + 1)));
    }

    @Test
    void threeNightsGiveThreeDatesWithCheckOutExcluded() {
        DateRange stay = range(1, 4);

        assertThat(stay.nights()).isEqualTo(3);
        assertThat(stay.dates()).hasSize(3).containsExactly(jan(1), jan(2), jan(3));
    }

    @Test
    void datesCrossMonthBoundary() {
        DateRange stay = new DateRange(LocalDate.of(2027, 1, 30), LocalDate.of(2027, 2, 2));

        assertThat(stay.dates()).containsExactly(LocalDate.of(2027, 1, 30), LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 1));
    }

    @Test
    void rangesThatOnlyTouchShareNoNight() {
        assertThat(range(1, 5).dates()).doesNotContainAnyElementsOf(range(5, 8).dates());
        assertThat(range(5, 8).dates()).doesNotContainAnyElementsOf(range(1, 5).dates());
    }

    @Test
    void containsIsHalfOpen() {
        DateRange stay = range(5, 8);

        assertThat(stay.contains(jan(4))).isFalse();
        assertThat(stay.contains(jan(5))).isTrue();
        assertThat(stay.contains(jan(7))).isTrue();
        assertThat(stay.contains(jan(8))).isFalse();
    }

    @Test
    void startsBefore() {
        DateRange stay = range(5, 8);

        assertThat(stay.startsBefore(jan(6))).isTrue();
        assertThat(stay.startsBefore(jan(5))).isFalse();
        assertThat(stay.startsBefore(jan(4))).isFalse();
    }
}
