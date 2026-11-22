package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import com.rupeek.hotel.domain.value.UpiDetails;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final Instant T0 = Instant.parse("2027-03-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
    private static final Money AMOUNT = Money.of("6000", "INR");

    private static Payment newPayment() {
        return Payment.initiate(BookingId.newId(), AMOUNT, new UpiDetails("asha@okbank"),
                new IdempotencyKey("key-1"), CLOCK);
    }

    @Test
    void initiateStartsInitiatedWithZeroRefunded() {
        Payment payment = newPayment();

        assertThat(payment.status()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(payment.gatewayReference()).isNull();
        assertThat(payment.failureReason()).isNull();
        assertThat(payment.createdAt()).isEqualTo(T0);
        assertThat(payment.isSuccessful()).isFalse();
    }

    @Test
    void zeroAmountIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Payment.initiate(
                BookingId.newId(), Money.zero(AMOUNT.currency()), new UpiDetails("asha@okbank"),
                new IdempotencyKey("key-0"), CLOCK));
    }

    @Test
    void methodDelegatesToTheInstrument() {
        assertThat(newPayment().method()).isEqualTo(PaymentMethodType.UPI);
    }

    @Test
    void successThenRefund() {
        Payment payment = newPayment();

        payment.markSucceeded("gw-123", CLOCK);
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.gatewayReference()).isEqualTo("gw-123");
        assertThat(payment.isSuccessful()).isTrue();

        Clock later = Clock.offset(CLOCK, Duration.ofMinutes(10));
        payment.recordRefund(Money.of("4500", "INR"), later);
        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.updatedAt()).isEqualTo(T0.plus(Duration.ofMinutes(10)));
        assertThat(payment.isSuccessful()).isFalse();
    }

    @Test
    void refundOfTheFullAmountIsAllowed() {
        Payment payment = newPayment();
        payment.markSucceeded("gw-123", CLOCK);

        payment.recordRefund(AMOUNT, CLOCK);

        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refundAboveTheAmountThrowsAndChangesNothing() {
        Payment payment = newPayment();
        payment.markSucceeded("gw-123", CLOCK);

        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> payment.recordRefund(Money.of("6000.01", "INR"), CLOCK));
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void refundInAnotherCurrencyThrows() {
        Payment payment = newPayment();
        payment.markSucceeded("gw-123", CLOCK);

        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> payment.recordRefund(Money.of("1", "USD"), CLOCK));
    }

    @Test
    void zeroRefundThrows() {
        Payment payment = newPayment();
        payment.markSucceeded("gw-123", CLOCK);

        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> payment.recordRefund(Money.zero(AMOUNT.currency()), CLOCK));
    }

    @Test
    void refundBeforeSuccessThrows() {
        Payment payment = newPayment();

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> payment.recordRefund(Money.of("10", "INR"), CLOCK));
    }

    @Test
    void refundOnARefundedPaymentThrows() {
        Payment payment = newPayment();
        payment.markSucceeded("gw-123", CLOCK);
        payment.recordRefund(Money.of("1000", "INR"), CLOCK);

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> payment.recordRefund(Money.of("1000", "INR"), CLOCK));
    }

    @Test
    void markSucceededTwiceThrows() {
        Payment payment = newPayment();
        payment.markSucceeded("gw-123", CLOCK);

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> payment.markSucceeded("gw-456", CLOCK));
        assertThat(payment.gatewayReference()).isEqualTo("gw-123");
    }

    @Test
    void markFailedRecordsTheReasonAndIsTerminal() {
        Payment payment = newPayment();

        payment.markFailed("insufficient funds", CLOCK);

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.failureReason()).isEqualTo("insufficient funds");
        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> payment.markSucceeded("gw-1", CLOCK));
    }

    @Test
    void blankGatewayReferenceOrReasonIsRejected() {
        Payment payment = newPayment();

        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> payment.markSucceeded(" ", CLOCK));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> payment.markFailed(null, CLOCK));
        assertThat(payment.status()).isEqualTo(PaymentStatus.INITIATED);
    }

    @Test
    void equalityUsesIdOnly() {
        Payment a = newPayment();

        assertThat(a).isEqualTo(a).isNotEqualTo(newPayment());
    }
}
