package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    private static final Currency INR = Currency.getInstance("INR");

    @Test
    void negativeAmountIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Money.of("-0.01", "INR"));
    }

    @Test
    void nullAmountOrCurrencyIsRejected() {
        assertThatNullPointerException().isThrownBy(() -> new Money(null, INR));
        assertThatNullPointerException().isThrownBy(() -> new Money(BigDecimal.ONE, null));
    }

    @Test
    void unknownCurrencyCodeIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Money.of("10", "XXXX"));
    }

    @Test
    void mixedCurrenciesAreRejected() {
        Money inr = Money.of("100", "INR");
        Money usd = Money.of("100", "USD");

        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> inr.plus(usd));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> inr.minus(usd));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> inr.isGreaterThan(usd));
    }

    @Test
    void minusBelowZeroIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> Money.of("10.00", "INR").minus(Money.of("10.01", "INR")));
    }

    @Test
    void minusToExactlyZeroIsAllowed() {
        assertThat(Money.of("10", "INR").minus(Money.of("10", "INR")).isZero()).isTrue();
    }

    @Test
    void hundredEqualsHundredPointZeroZero() {
        assertThat(Money.of("100", "INR")).isEqualTo(Money.of("100.00", "INR"));
        assertThat(Money.of("100", "INR")).hasSameHashCodeAs(Money.of("100.00", "INR"));
    }

    @Test
    void percentFiftyOf999Point99IsFiveHundred() {
        // 999.99 * 50 / 100 = 499.995, an exact tie. HALF_EVEN rounds it to the even digit: 500.00
        assertThat(Money.of("999.99", "INR").percent(50)).isEqualTo(Money.of("500.00", "INR"));
    }

    @ParameterizedTest
    @CsvSource({"0.125, 0.12", "0.135, 0.14", "0.115, 0.12", "0.1251, 0.13", "2.5, 2.50"})
    void scaleIsTwoWithHalfEven(String input, String expected) {
        assertThat(Money.of(input, "INR")).isEqualTo(Money.of(expected, "INR"));
        assertThat(Money.of(input, "INR").amount().scale()).isEqualTo(2);
    }

    @Test
    void percentRejectsValuesOutsideZeroToHundred() {
        Money money = Money.of("100", "INR");

        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> money.percent(-1));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> money.percent(101));
        assertThat(money.percent(0).isZero()).isTrue();
        assertThat(money.percent(100)).isEqualTo(money);
    }

    @Test
    void plusAndTimes() {
        assertThat(Money.of("10.50", "INR").plus(Money.of("0.75", "INR"))).isEqualTo(Money.of("11.25", "INR"));
        assertThat(Money.of("10.50", "INR").times(3)).isEqualTo(Money.of("31.50", "INR"));
        assertThat(Money.of("10.50", "INR").times(0).isZero()).isTrue();
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Money.of("1", "INR").times(-1));
    }

    @Test
    void zeroAndComparison() {
        assertThat(Money.zero(INR).isZero()).isTrue();
        assertThat(Money.of("0.01", "INR").isZero()).isFalse();
        assertThat(Money.of("2", "INR").isGreaterThan(Money.of("1.99", "INR"))).isTrue();
        assertThat(Money.of("2", "INR").isGreaterThan(Money.of("2.00", "INR"))).isFalse();
    }

    @Test
    void toStringShowsCurrencyAndAmount() {
        assertThat(Money.of("5", "INR")).hasToString("INR 5.00");
    }
}
