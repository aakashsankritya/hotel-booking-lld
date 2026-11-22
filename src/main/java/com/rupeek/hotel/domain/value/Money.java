package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount of money in one currency. Immutable.
 *
 * <p>The amount is never negative. Its scale is always 2 and it uses {@link RoundingMode#HALF_EVEN},
 * so {@code 100} and {@code 100.00} are equal. A fixed scale of 2 is an assumption: it is wrong for
 * currencies with 0 or 3 minor digits (JPY, KWD). The project uses INR, so this is acceptable.
 */
public record Money(BigDecimal amount, Currency currency) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new DomainValidationException("amount must not be negative: " + amount);
        }
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(String amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        try {
            return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
        } catch (IllegalArgumentException e) { // bad number (NumberFormatException) or unknown currency code
            throw new DomainValidationException("invalid money: " + amount + " " + currencyCode);
        }
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /** Throws if the result would be below zero. */
    public Money minus(Money other) {
        requireSameCurrency(other);
        if (amount.compareTo(other.amount) < 0) {
            throw new DomainValidationException("result would be negative: " + this + " - " + other);
        }
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money times(int multiplier) {
        if (multiplier < 0) {
            throw new DomainValidationException("multiplier must not be negative: " + multiplier);
        }
        return new Money(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    /** Returns {@code percent} % of this amount, rounded HALF_EVEN to 2 places. */
    public Money percent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new DomainValidationException("percent must be between 0 and 100: " + percent);
        }
        BigDecimal result = amount.multiply(BigDecimal.valueOf(percent)).divide(HUNDRED, SCALE, ROUNDING);
        return new Money(result, currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new DomainValidationException(
                    "currency mismatch: " + currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
        }
    }
}
