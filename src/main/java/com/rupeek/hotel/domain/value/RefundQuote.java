package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;

/**
 * Result of a cancellation policy: how much to refund and how much to keep.
 *
 * <p>Create it with {@link #of(Money, int, String)}. That factory guarantees
 * {@code refundAmount + cancellationFee == total}: the fee is the total minus the rounded refund,
 * so no cent is lost or created by rounding.
 */
public record RefundQuote(Money refundAmount, Money cancellationFee, String reason) {

    public RefundQuote {
        Objects.requireNonNull(refundAmount, "refundAmount must not be null");
        Objects.requireNonNull(cancellationFee, "cancellationFee must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (!refundAmount.currency().equals(cancellationFee.currency())) {
            throw new DomainValidationException("refund and fee must use the same currency");
        }
    }

    public static RefundQuote of(Money total, int refundPercent, String reason) {
        Objects.requireNonNull(total, "total must not be null");
        Money refund = total.percent(refundPercent); // throws if refundPercent is outside 0..100
        return new RefundQuote(refund, total.minus(refund), reason);
    }
}
