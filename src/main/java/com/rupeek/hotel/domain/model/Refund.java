package com.rupeek.hotel.domain.model;

import static com.rupeek.hotel.domain.model.RefundStatus.TABLE;

import com.rupeek.hotel.domain.common.Validations;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentId;
import com.rupeek.hotel.domain.value.RefundId;
import com.rupeek.hotel.domain.value.RefundQuote;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Money returned to the guest for one payment. This class does not change the {@link Payment}.
 * The service calls {@link Payment#recordRefund(Money, Clock)} itself. If the quote refunds
 * nothing, the service creates no refund. Not thread-safe.
 */
public final class Refund {

    private static final String ENTITY = "Refund";

    private final RefundId id;
    private final PaymentId paymentId;
    private final BookingId bookingId;
    private final Money amount;
    private final Instant createdAt;
    private RefundStatus status;
    private String gatewayReference;
    private String failureReason;
    private Instant updatedAt;

    private Refund(RefundId id, PaymentId paymentId, BookingId bookingId, Money amount, Instant now) {
        this.id = id;
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.createdAt = now;
        this.updatedAt = now;
        this.status = RefundStatus.INITIATED;
    }

    /**
     * @throws InvalidStateTransitionException if the payment is not {@code SUCCEEDED}
     * @throws DomainValidationException if the refund amount is zero or more than the payment amount
     */
    public static Refund initiate(Payment payment, RefundQuote quote, Clock clock) {
        Objects.requireNonNull(payment, "payment must not be null");
        Objects.requireNonNull(quote, "quote must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        if (!payment.isSuccessful()) {
            throw new InvalidStateTransitionException("Payment", payment.id(), payment.status(),
                    PaymentStatus.REFUNDED, "only a successful payment can be refunded");
        }
        Money amount = quote.refundAmount();
        if (amount.isZero()) {
            throw new DomainValidationException("refund amount must be greater than zero");
        }
        if (amount.isGreaterThan(payment.amount())) {
            throw new DomainValidationException(
                    "refund " + amount + " is more than the payment amount " + payment.amount());
        }
        return new Refund(RefundId.newId(), payment.id(), payment.bookingId(), amount, clock.instant());
    }

    /** {@code INITIATED} → {@code SUCCEEDED}. */
    public void markSucceeded(String ref, Clock clock) {
        String reference = Validations.requireNotBlank(ref, "ref");
        transitionTo(RefundStatus.SUCCEEDED, clock.instant());
        gatewayReference = reference;
    }

    /** {@code INITIATED} → {@code FAILED}. */
    public void markFailed(String reason, Clock clock) {
        String cleanReason = Validations.requireNotBlank(reason, "reason");
        transitionTo(RefundStatus.FAILED, clock.instant());
        failureReason = cleanReason;
    }

    private void transitionTo(RefundStatus next, Instant now) {
        status = TABLE.requireTransition(ENTITY, id, status, next);
        updatedAt = now;
    }

    public RefundId id() {
        return id;
    }

    public PaymentId paymentId() {
        return paymentId;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public Money amount() {
        return amount;
    }

    public RefundStatus status() {
        return status;
    }

    /** Null until the refund succeeds. */
    public String gatewayReference() {
        return gatewayReference;
    }

    /** Null unless the refund failed. */
    public String failureReason() {
        return failureReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Refund other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Refund[" + id + ", " + status + "]";
    }
}
