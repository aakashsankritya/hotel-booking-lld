package com.rupeek.hotel.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.rupeek.hotel.application.MutableClock;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import com.rupeek.hotel.infrastructure.payment.CardPaymentProcessor;
import com.rupeek.hotel.infrastructure.payment.MockPaymentGateway;
import com.rupeek.hotel.infrastructure.payment.UpiPaymentProcessor;
import com.rupeek.hotel.infrastructure.payment.WalletPaymentProcessor;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentProcessorRegistryTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-01T06:00:00Z"), ZoneOffset.UTC);
    private final PaymentProcessor card = new CardPaymentProcessor(gateway, clock);
    private final PaymentProcessor upi = new UpiPaymentProcessor(gateway);
    private final PaymentProcessor wallet = new WalletPaymentProcessor(gateway);

    @Test
    void findsEachProcessorByItsType() {
        PaymentProcessorRegistry registry = new PaymentProcessorRegistry(List.of(card, upi, wallet));

        assertThat(registry.forType(PaymentMethodType.CARD)).isSameAs(card);
        assertThat(registry.forType(PaymentMethodType.UPI)).isSameAs(upi);
        assertThat(registry.forType(PaymentMethodType.WALLET)).isSameAs(wallet);
    }

    @Test
    void missingTypeThrows() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PaymentProcessorRegistry(List.of(card, upi)))
                .withMessageContaining("WALLET");
    }

    @Test
    void duplicateTypeThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PaymentProcessorRegistry(List.of(card, upi, wallet, new UpiPaymentProcessor(gateway))))
                .withMessageContaining("UPI");
    }
}
