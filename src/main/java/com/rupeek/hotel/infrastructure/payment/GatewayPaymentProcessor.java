package com.rupeek.hotel.infrastructure.payment;

import com.rupeek.hotel.domain.payment.GatewayRequest;
import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.payment.PaymentGateway;
import com.rupeek.hotel.domain.payment.PaymentProcessor;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PaymentInstrument;
import com.rupeek.hotel.domain.value.PaymentMethodType;

/**
 * Shared steps of a processor: check the instrument type, build the gateway request, delegate the refund.
 * A method supplies its {@link #token(PaymentInstrument)}. A method with its own rules, such as card expiry,
 * overrides {@link #chargeInstrument}.
 */
abstract class GatewayPaymentProcessor<T extends PaymentInstrument> implements PaymentProcessor {

    private final PaymentGateway gateway;
    private final PaymentMethodType type;
    private final Class<T> instrumentType;

    protected GatewayPaymentProcessor(PaymentGateway gateway, PaymentMethodType type, Class<T> instrumentType) {
        this.gateway = gateway;
        this.type = type;
        this.instrumentType = instrumentType;
    }

    @Override
    public final PaymentMethodType type() {
        return type;
    }

    @Override
    public final GatewayResult charge(Money amount, PaymentInstrument instrument, IdempotencyKey key) {
        if (!instrumentType.isInstance(instrument)) {
            throw new IllegalArgumentException(type + " processor got " + instrument.type());
        }
        return chargeInstrument(amount, instrumentType.cast(instrument), key);
    }

    @Override
    public final GatewayResult refund(String gatewayReference, Money amount) {
        return gateway.refund(gatewayReference, amount);
    }

    /** The default: send the token to the gateway. */
    protected GatewayResult chargeInstrument(Money amount, T instrument, IdempotencyKey key) {
        return gateway.charge(new GatewayRequest(type, amount, token(instrument), key));
    }

    /** What the gateway gets in place of the raw instrument. */
    protected abstract String token(T instrument);
}
