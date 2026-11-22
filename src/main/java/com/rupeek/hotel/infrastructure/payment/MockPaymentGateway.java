package com.rupeek.hotel.infrastructure.payment;

import com.rupeek.hotel.domain.payment.GatewayRequest;
import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.payment.PaymentGateway;
import com.rupeek.hotel.domain.value.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * A fake gateway with fixed rules. A token that ends in {@code 0000} or contains {@code fail} is declined.
 * Everything else succeeds. Refunds always succeed.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResult charge(GatewayRequest request) {
        String token = request.instrumentToken();
        if (token.endsWith("0000") || token.contains("fail")) {
            return GatewayResult.failure("declined");
        }
        return GatewayResult.success("gw-" + UUID.randomUUID());
    }

    @Override
    public GatewayResult refund(String reference, Money amount) {
        return GatewayResult.success("rf-" + UUID.randomUUID());
    }
}
