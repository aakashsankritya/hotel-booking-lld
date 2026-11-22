package com.rupeek.hotel.api.dto.payment;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.value.CardDetails;
import com.rupeek.hotel.domain.value.PaymentInstrument;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import com.rupeek.hotel.domain.value.UpiDetails;
import com.rupeek.hotel.domain.value.WalletDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** {@code method} says which of the three detail objects must be present. A new method is a new case. */
public record PaymentRequest(
        @NotNull PaymentMethodType method,
        @Valid CardRequest card,
        @Valid UpiRequest upi,
        @Valid WalletRequest wallet) {

    public PaymentInstrument toInstrument() {
        return switch (method) {
            case CARD -> {
                CardRequest c = required(card, "card");
                yield new CardDetails(c.last4(), c.expiry(), c.holderName());
            }
            case UPI -> new UpiDetails(required(upi, "upi").vpa());
            case WALLET -> {
                WalletRequest w = required(wallet, "wallet");
                yield new WalletDetails(w.provider(), w.walletId());
            }
        };
    }

    private <T> T required(T details, String field) {
        if (details == null) {
            throw new DomainValidationException("'" + field + "' is required when method is " + method);
        }
        return details;
    }
}
