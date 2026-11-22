package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

class PriceRangeTest {

    private static Money inr(String amount) {
        return Money.of(amount, "INR");
    }

    @Test
    void minGreaterThanMaxIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new PriceRange(inr("200"), inr("100")));
    }

    @Test
    void mixedCurrenciesAreRejected() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> new PriceRange(inr("100"), Money.of("200", "USD")));
    }

    @Test
    void minEqualToMaxIsAllowed() {
        assertThat(new PriceRange(inr("100"), inr("100")).contains(inr("100"))).isTrue();
    }

    @Test
    void bothBoundsAreInclusive() {
        PriceRange range = new PriceRange(inr("1000"), inr("5000"));

        assertThat(range.contains(inr("999.99"))).isFalse();
        assertThat(range.contains(inr("1000"))).isTrue();
        assertThat(range.contains(inr("3000"))).isTrue();
        assertThat(range.contains(inr("5000"))).isTrue();
        assertThat(range.contains(inr("5000.01"))).isFalse();
    }

    @Test
    void openMinMeansNoLowerLimit() {
        PriceRange range = new PriceRange(null, inr("5000"));

        assertThat(range.contains(Money.zero(inr("0").currency()))).isTrue();
        assertThat(range.contains(inr("5000.01"))).isFalse();
    }

    @Test
    void openMaxMeansNoUpperLimit() {
        PriceRange range = new PriceRange(inr("1000"), null);

        assertThat(range.contains(inr("999.99"))).isFalse();
        assertThat(range.contains(inr("9999999"))).isTrue();
    }

    @Test
    void bothOpenContainsEverything() {
        assertThat(new PriceRange(null, null).contains(inr("123456"))).isTrue();
    }
}
