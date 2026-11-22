package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RefundQuote;
import com.rupeek.hotel.domain.value.UpiDetails;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RefundTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2027-03-01T10:00:00Z"), ZoneOffset.UTC);
    private static final Money AMOUNT = Money.of("6000", "INR");

    private static Payment succeededPayment() {
        Payment payment = Payment.initiate(BookingId.newId(), AMOUNT, new UpiDetails("asha@okbank"),
                new IdempotencyKey("key-1"), CLOCK);
        payment.markSucceeded("gw-1", CLOCK);
        return payment;
    }

    @Test
    void initiateCopiesIdsAndTheQuoteAmount() {
        Payment payment = succeededPayment();

        Refund refund = Refund.initiate(payment, RefundQuote.of(AMOUNT, 50, "moderate policy"), CLOCK);

        assertThat(refund.status()).isEqualTo(RefundStatus.INITIATED);
        assertThat(refund.amount()).isEqualTo(Money.of("3000", "INR"));
        assertThat(refund.paymentId()).isEqualTo(payment.id());
        assertThat(refund.bookingId()).isEqualTo(payment.bookingId());
        assertThat(refund.createdAt()).isEqualTo(CLOCK.instant());
        assertThat(refund.gatewayReference()).isNull();
    }

    @Test
    void zeroAmountThrows() {
        Payment payment = succeededPayment();

        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> Refund.initiate(payment, RefundQuote.of(AMOUNT, 0, "non-refundable"), CLOCK));
    }

    @Test
    void refundOnAFailedPaymentThrows() {
        Payment failed = Payment.initiate(BookingId.newId(), AMOUNT, new UpiDetails("asha@okbank"),
                new IdempotencyKey("key-2"), CLOCK);
        failed.markFailed("declined", CLOCK);

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> Refund.initiate(failed, RefundQuote.of(AMOUNT, 100, "full"), CLOCK));
    }

    @Test
    void refundOnAnInitiatedPaymentThrows() {
        Payment initiated = Payment.initiate(BookingId.newId(), AMOUNT, new UpiDetails("asha@okbank"),
                new IdempotencyKey("key-3"), CLOCK);

        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> Refund.initiate(initiated, RefundQuote.of(AMOUNT, 100, "full"), CLOCK));
    }

    @Test
    void refundLargerThanThePaymentThrows() {
        Payment payment = succeededPayment();
        RefundQuote tooBig = new RefundQuote(Money.of("6000.01", "INR"), Money.zero(AMOUNT.currency()), "bad quote");

        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> Refund.initiate(payment, tooBig, CLOCK));
    }

    @Test
    void markSucceededStoresTheReferenceAndIsTerminal() {
        Refund refund = Refund.initiate(succeededPayment(), RefundQuote.of(AMOUNT, 100, "full"), CLOCK);

        refund.markSucceeded("rf-1", CLOCK);

        assertThat(refund.status()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(refund.gatewayReference()).isEqualTo("rf-1");
        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> refund.markFailed("late failure", CLOCK));
    }

    @Test
    void markFailedStoresTheReasonAndIsTerminal() {
        Refund refund = Refund.initiate(succeededPayment(), RefundQuote.of(AMOUNT, 100, "full"), CLOCK);

        refund.markFailed("gateway down", CLOCK);

        assertThat(refund.status()).isEqualTo(RefundStatus.FAILED);
        assertThat(refund.failureReason()).isEqualTo("gateway down");
        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> refund.markSucceeded("rf-1", CLOCK));
    }

    @Test
    void equalityUsesIdOnly() {
        Payment payment = succeededPayment();
        Refund a = Refund.initiate(payment, RefundQuote.of(AMOUNT, 100, "full"), CLOCK);
        Refund b = Refund.initiate(payment, RefundQuote.of(AMOUNT, 100, "full"), CLOCK);

        assertThat(a).isEqualTo(a).isNotEqualTo(b);
    }
}
