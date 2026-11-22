package com.rupeek.hotel.domain.payment;

import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentMethodType;

/** The gateway gets a token, never the raw instrument. */
public record GatewayRequest(PaymentMethodType method, Money amount, String instrumentToken, IdempotencyKey key) {
}
