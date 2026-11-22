package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RefundQuoteTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 25, 33, 50, 67, 99, 100})
    void refundPlusFeeEqualsTotal(int percent) {
        Money total = Money.of("999.99", "INR");

        RefundQuote quote = RefundQuote.of(total, percent, "policy");

        assertThat(quote.refundAmount().plus(quote.cancellationFee())).isEqualTo(total);
    }

    @Test
    void fiftyPercentOfOddAmount() {
        RefundQuote quote = RefundQuote.of(Money.of("999.99", "INR"), 50, "half refund");

        assertThat(quote.refundAmount()).isEqualTo(Money.of("500.00", "INR"));
        assertThat(quote.cancellationFee()).isEqualTo(Money.of("499.99", "INR"));
        assertThat(quote.reason()).isEqualTo("half refund");
    }

    @Test
    void fullAndZeroRefund() {
        Money total = Money.of("100", "INR");

        assertThat(RefundQuote.of(total, 100, "full").cancellationFee().isZero()).isTrue();
        assertThat(RefundQuote.of(total, 0, "none").refundAmount().isZero()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101, 1000})
    void percentOutsideZeroToHundredIsRejected(int percent) {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> RefundQuote.of(Money.of("100", "INR"), percent, "x"));
    }

    @Test
    void mixedCurrenciesAreRejected() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> new RefundQuote(Money.of("1", "INR"), Money.of("1", "USD"), "x"));
    }
}
