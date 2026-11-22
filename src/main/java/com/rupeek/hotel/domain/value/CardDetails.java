package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Card instrument. Stores only the last 4 digits. Never add the full number or the CVV here.
 *
 * <p>The constructor does not check the expiry date, because that needs a clock.
 * The payment processor calls {@link #isExpired(Clock)}.
 */
public record CardDetails(String last4, YearMonth expiry, String holderName) implements PaymentInstrument {

    private static final Pattern LAST4 = Pattern.compile("[0-9]{4}");

    public CardDetails {
        Objects.requireNonNull(last4, "last4 must not be null");
        Objects.requireNonNull(expiry, "expiry must not be null");
        Objects.requireNonNull(holderName, "holderName must not be null");
        if (!LAST4.matcher(last4).matches()) {
            throw new DomainValidationException("last4 must be exactly 4 digits");
        }
        if (holderName.isBlank()) {
            throw new DomainValidationException("holderName must not be blank");
        }
    }

    /** A card is valid until the end of its expiry month. */
    public boolean isExpired(Clock clock) {
        return expiry.isBefore(YearMonth.now(clock));
    }

    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.CARD;
    }
}
