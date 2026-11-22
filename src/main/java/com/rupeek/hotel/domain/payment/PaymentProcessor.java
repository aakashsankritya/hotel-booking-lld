package com.rupeek.hotel.domain.payment;

import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentInstrument;
import com.rupeek.hotel.domain.value.PaymentMethodType;

/** Knows how to charge and refund one payment method. Add a class for a new method. */
public interface PaymentProcessor {

    PaymentMethodType type();

    GatewayResult charge(Money amount, PaymentInstrument instrument, IdempotencyKey key);

    GatewayResult refund(String gatewayReference, Money amount);
}
