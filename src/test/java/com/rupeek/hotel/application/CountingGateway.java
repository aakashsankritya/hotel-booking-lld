package com.rupeek.hotel.application;

import com.rupeek.hotel.domain.payment.GatewayRequest;
import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.payment.PaymentGateway;
import com.rupeek.hotel.domain.value.Money;
import java.util.concurrent.atomic.AtomicInteger;

/** Test gateway. It passes calls to another gateway and counts the charges. It can also be told to throw. */
public final class CountingGateway implements PaymentGateway {

    private final PaymentGateway delegate;
    private final AtomicInteger charges = new AtomicInteger();
    private volatile RuntimeException chargeError;
    private volatile RuntimeException refundError;

    public CountingGateway(PaymentGateway delegate) {
        this.delegate = delegate;
    }

    @Override
    public GatewayResult charge(GatewayRequest request) {
        charges.incrementAndGet();
        if (chargeError != null) {
            throw chargeError;
        }
        return delegate.charge(request);
    }

    @Override
    public GatewayResult refund(String reference, Money amount) {
        if (refundError != null) {
            throw refundError;
        }
        return delegate.refund(reference, amount);
    }

    public int chargeCount() {
        return charges.get();
    }

    public void throwOnCharge(RuntimeException error) {
        this.chargeError = error;
    }

    public void throwOnRefund(RuntimeException error) {
        this.refundError = error;
    }
}
