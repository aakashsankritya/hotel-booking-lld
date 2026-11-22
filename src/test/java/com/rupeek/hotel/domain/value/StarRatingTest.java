package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StarRatingTest {

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 6, Integer.MAX_VALUE})
    void outOfRangeIsRejected(int value) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new StarRating(value));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void inRangeIsAccepted(int value) {
        assertThat(new StarRating(value).value()).isEqualTo(value);
    }

    @Test
    void isAtLeast() {
        assertThat(new StarRating(4).isAtLeast(new StarRating(3))).isTrue();
        assertThat(new StarRating(4).isAtLeast(new StarRating(4))).isTrue();
        assertThat(new StarRating(4).isAtLeast(new StarRating(5))).isFalse();
    }
}
