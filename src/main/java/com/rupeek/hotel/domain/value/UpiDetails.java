package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;
import java.util.regex.Pattern;

/** UPI instrument. {@code vpa} is a virtual payment address, for example {@code name@bank}. */
public record UpiDetails(String vpa) implements PaymentInstrument {

    private static final Pattern VPA = Pattern.compile("^[\\w.\\-]{2,}@[a-zA-Z]{2,}$");

    public UpiDetails {
        Objects.requireNonNull(vpa, "vpa must not be null");
        if (!VPA.matcher(vpa).matches()) {
            throw new DomainValidationException("invalid UPI address: " + vpa);
        }
    }

    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.UPI;
    }
}
