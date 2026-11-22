package com.rupeek.hotel.infrastructure.payment;

import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.payment.PaymentGateway;
import com.rupeek.hotel.domain.value.CardDetails;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentProcessor extends GatewayPaymentProcessor<CardDetails> {

    private final Clock clock;

    public CardPaymentProcessor(PaymentGateway gateway, Clock clock) {
        super(gateway, PaymentMethodType.CARD, CardDetails.class);
        this.clock = clock;
    }

    @Override
    protected GatewayResult chargeInstrument(Money amount, CardDetails card, IdempotencyKey key) {
        if (card.isExpired(clock)) {
            return GatewayResult.failure("card expired"); // no gateway call
        }
        return super.chargeInstrument(amount, card, key);
    }

    @Override
    protected String token(CardDetails card) {
        return "card-" + card.last4();
    }
}
