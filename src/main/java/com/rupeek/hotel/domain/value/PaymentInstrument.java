package com.rupeek.hotel.domain.value;

/**
 * How a guest pays. Sealed, so a {@code switch} over it is checked for completeness.
 * No implementation stores a full card number or a CVV.
 */
public sealed interface PaymentInstrument permits CardDetails, UpiDetails, WalletDetails {

    PaymentMethodType type();
}
