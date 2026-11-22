package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.Objects;

/** Wallet instrument, for example provider {@code paytm} and the guest's wallet id. */
public record WalletDetails(String provider, String walletId) implements PaymentInstrument {

    public WalletDetails {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        if (provider.isBlank()) {
            throw new DomainValidationException("provider must not be blank");
        }
        if (walletId.isBlank()) {
            throw new DomainValidationException("walletId must not be blank");
        }
    }

    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.WALLET;
    }
}
