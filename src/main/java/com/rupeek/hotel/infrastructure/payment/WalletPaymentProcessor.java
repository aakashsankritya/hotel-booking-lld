package com.rupeek.hotel.infrastructure.payment;

import com.rupeek.hotel.domain.payment.PaymentGateway;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import com.rupeek.hotel.domain.value.WalletDetails;
import org.springframework.stereotype.Component;

@Component
public class WalletPaymentProcessor extends GatewayPaymentProcessor<WalletDetails> {

    public WalletPaymentProcessor(PaymentGateway gateway) {
        super(gateway, PaymentMethodType.WALLET, WalletDetails.class);
    }

    @Override
    protected String token(WalletDetails wallet) {
        return "wallet-" + wallet.provider() + "-" + wallet.walletId();
    }
}
