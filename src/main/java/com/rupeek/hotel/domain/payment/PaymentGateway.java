package com.rupeek.hotel.domain.payment;

/** The outside payment system. The port of the application. */
public interface PaymentGateway {

    GatewayResult charge(GatewayRequest request);

    GatewayResult refund(String reference, com.rupeek.hotel.domain.value.Money amount);
}
