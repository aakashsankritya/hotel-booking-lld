package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.time.Duration;
import java.util.Objects;

/**
 * One step of the tiered cancellation policy: if the guest cancels at least
 * {@code minNoticeBeforeCheckIn} before check-in, {@code refundPercent} % is refunded.
 */
public record CancellationTier(Duration minNoticeBeforeCheckIn, int refundPercent) {

    public CancellationTier {
        Objects.requireNonNull(minNoticeBeforeCheckIn, "minNoticeBeforeCheckIn must not be null");
        if (minNoticeBeforeCheckIn.isNegative()) {
            throw new DomainValidationException("minNoticeBeforeCheckIn must not be negative");
        }
        if (refundPercent < 0 || refundPercent > 100) {
            throw new DomainValidationException("refundPercent must be between 0 and 100: " + refundPercent);
        }
    }
}
