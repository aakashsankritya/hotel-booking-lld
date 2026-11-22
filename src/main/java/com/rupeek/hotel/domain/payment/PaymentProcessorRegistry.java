package com.rupeek.hotel.domain.payment;

import com.rupeek.hotel.domain.value.PaymentMethodType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Finds the processor for a payment method. Start-up fails if a method has no processor or two. */
public final class PaymentProcessorRegistry {

    private final Map<PaymentMethodType, PaymentProcessor> processors = new EnumMap<>(PaymentMethodType.class);

    public PaymentProcessorRegistry(List<PaymentProcessor> all) {
        for (PaymentProcessor processor : all) {
            if (processors.put(processor.type(), processor) != null) {
                throw new IllegalArgumentException("Two payment processors for " + processor.type());
            }
        }
        for (PaymentMethodType type : PaymentMethodType.values()) {
            if (!processors.containsKey(type)) {
                throw new IllegalArgumentException("No payment processor for " + type);
            }
        }
    }

    public PaymentProcessor forType(PaymentMethodType type) {
        return processors.get(type); // never null: the constructor checked every type
    }
}
