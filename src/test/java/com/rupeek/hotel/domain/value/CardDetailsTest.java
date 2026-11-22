package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CardDetailsTest {

    private static final YearMonth EXPIRY = YearMonth.of(2027, 6);

    private static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000", "1234", "9999"})
    void validLast4IsAccepted(String last4) {
        assertThat(new CardDetails(last4, EXPIRY, "A Kumar").last4()).isEqualTo(last4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345", "12a4", "12 4", " 123", "4111111111111111", "١٢٣٤"})
    void invalidLast4IsRejected(String last4) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new CardDetails(last4, EXPIRY, "A Kumar"));
    }

    @Test
    void nullFieldsAreRejected() {
        assertThatNullPointerException().isThrownBy(() -> new CardDetails(null, EXPIRY, "A Kumar"));
        assertThatNullPointerException().isThrownBy(() -> new CardDetails("1234", null, "A Kumar"));
        assertThatNullPointerException().isThrownBy(() -> new CardDetails("1234", EXPIRY, null));
    }

    @Test
    void blankHolderNameIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new CardDetails("1234", EXPIRY, " "));
    }

    @Test
    void cardIsValidThroughItsExpiryMonth() {
        CardDetails card = new CardDetails("1234", EXPIRY, "A Kumar");

        assertThat(card.isExpired(clockAt("2027-05-31T23:59:59Z"))).isFalse();
        assertThat(card.isExpired(clockAt("2027-06-30T23:59:59Z"))).isFalse();
        assertThat(card.isExpired(clockAt("2027-07-01T00:00:00Z"))).isTrue();
    }

    @Test
    void typeIsCard() {
        assertThat(new CardDetails("1234", EXPIRY, "A Kumar").type()).isEqualTo(PaymentMethodType.CARD);
    }
}
