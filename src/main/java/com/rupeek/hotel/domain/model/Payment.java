package com.rupeek.hotel.domain.model;

import static com.rupeek.hotel.domain.model.PaymentStatus.TABLE;

import com.rupeek.hotel.domain.common.Validations;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentId;
import com.rupeek.hotel.domain.value.PaymentInstrument;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * A payment for one booking. The instrument holds only masked data (last 4 digits, UPI address,
 * wallet id), never a full card number or CVV. Not thread-safe.
 *
 * <p>A payment is refunded once, in full or in part: {@code REFUNDED} is a terminal state.
 */
public final class Payment {

    private static final String ENTITY = "Payment";

    private final PaymentId id;
    private final BookingId bookingId;
    private final Money amount;
    private final PaymentInstrument instrument;
    private final IdempotencyKey idempotencyKey;
    private final Instant createdAt;
    private PaymentStatus status;
    private String gatewayReference;
    private String failureReason;
    private Instant updatedAt;

    private Payment(PaymentId id, BookingId bookingId, Money amount, PaymentInstrument instrument,
            IdempotencyKey idempotencyKey, Instant now) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.instrument = instrument;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = now;
        this.updatedAt = now;
        this.status = PaymentStatus.INITIATED;
    }

    /** @throws DomainValidationException if the amount is zero */
    public static Payment initiate(BookingId bookingId, Money amount, PaymentInstrument instrument,
            IdempotencyKey idempotencyKey, Clock clock) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(instrument, "instrument must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        if (amount.isZero()) {
            throw new DomainValidationException("payment amount must be greater than zero");
        }
        return new Payment(PaymentId.newId(), bookingId, amount, instrument, idempotencyKey, clock.instant());
    }

    public PaymentMethodType method() {
        return instrument.type();
    }

    /** {@code INITIATED} → {@code SUCCEEDED}. */
    public void markSucceeded(String gatewayRef, Clock clock) {
        String reference = Validations.requireNotBlank(gatewayRef, "gatewayRef");
        transitionTo(PaymentStatus.SUCCEEDED, clock.instant());
        gatewayReference = reference;
    }

    /** {@code INITIATED} → {@code FAILED}. */
    public void markFailed(String reason, Clock clock) {
        String cleanReason = Validations.requireNotBlank(reason, "reason");
        transitionTo(PaymentStatus.FAILED, clock.instant());
        failureReason = cleanReason;
    }

    /**
     * {@code SUCCEEDED} → {@code REFUNDED}. The refund is greater than zero and not more than the
     * payment amount. It may be a part of the amount, but there is only one refund per payment.
     */
    public void recordRefund(Money refund, Clock clock) {
        Objects.requireNonNull(refund, "refund must not be null");
        if (refund.isZero()) {
            throw new DomainValidationException("refund must be greater than zero");
        }
        if (refund.isGreaterThan(amount)) {
            throw new DomainValidationException("refund " + refund + " is more than the payment amount " + amount);
        }
        transitionTo(PaymentStatus.REFUNDED, clock.instant());
    }

    /** True while the money is captured and not refunded: status is {@code SUCCEEDED}. */
    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCEEDED;
    }

    private void transitionTo(PaymentStatus next, Instant now) {
        status = TABLE.requireTransition(ENTITY, id, status, next);
        updatedAt = now;
    }

    public PaymentId id() {
        return id;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentInstrument instrument() {
        return instrument;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public PaymentStatus status() {
        return status;
    }

    /** Null until the payment succeeds. */
    public String gatewayReference() {
        return gatewayReference;
    }

    /** Null unless the payment failed. */
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
        return this == o || (o instanceof Payment other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Payment[" + id + ", " + status + "]";
    }
}
