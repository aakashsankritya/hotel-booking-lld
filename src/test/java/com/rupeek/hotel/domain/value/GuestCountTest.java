package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GuestCountTest {

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 21, Integer.MAX_VALUE})
    void outOfRangeIsRejected(int value) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new GuestCount(value));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 19, 20})
    void inRangeIsAccepted(int value) {
        assertThat(new GuestCount(value).value()).isEqualTo(value);
    }

    @Test
    void fitsIn() {
        assertThat(new GuestCount(3).fitsIn(3)).isTrue();
        assertThat(new GuestCount(3).fitsIn(4)).isTrue();
        assertThat(new GuestCount(3).fitsIn(2)).isFalse();
    }
}
