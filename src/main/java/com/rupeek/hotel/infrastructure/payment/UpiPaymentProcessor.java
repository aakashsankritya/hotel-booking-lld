package com.rupeek.hotel.infrastructure.payment;

import com.rupeek.hotel.domain.payment.PaymentGateway;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import com.rupeek.hotel.domain.value.UpiDetails;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentProcessor extends GatewayPaymentProcessor<UpiDetails> {

    public UpiPaymentProcessor(PaymentGateway gateway) {
        super(gateway, PaymentMethodType.UPI, UpiDetails.class);
    }

    @Override
    protected String token(UpiDetails upi) {
        return "upi-" + upi.vpa();
    }
}
