package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaymentAndRefundStatusTest {

    private static final Map<PaymentStatus, Set<PaymentStatus>> PAYMENT = Map.of(
            PaymentStatus.INITIATED, Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED),
            PaymentStatus.SUCCEEDED, Set.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, Set.of(),
            PaymentStatus.REFUNDED, Set.of());

    private static final Map<RefundStatus, Set<RefundStatus>> REFUND = Map.of(
            RefundStatus.INITIATED, Set.of(RefundStatus.SUCCEEDED, RefundStatus.FAILED),
            RefundStatus.SUCCEEDED, Set.of(),
            RefundStatus.FAILED, Set.of());

    static Stream<Arguments> paymentPairs() {
        return Stream.of(PaymentStatus.values()).flatMap(from -> Stream.of(PaymentStatus.values())
                .map(to -> Arguments.of(from, to, PAYMENT.get(from).contains(to))));
    }

    static Stream<Arguments> refundPairs() {
        return Stream.of(RefundStatus.values()).flatMap(from -> Stream.of(RefundStatus.values())
                .map(to -> Arguments.of(from, to, REFUND.get(from).contains(to))));
    }

    @ParameterizedTest(name = "payment {0} -> {1} allowed: {2}")
    @MethodSource("paymentPairs")
    void paymentTransitionsMatchTheTable(PaymentStatus from, PaymentStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        assertThat(from.isTerminal()).isEqualTo(PAYMENT.get(from).isEmpty());
    }

    @ParameterizedTest(name = "refund {0} -> {1} allowed: {2}")
    @MethodSource("refundPairs")
    void refundTransitionsMatchTheTable(RefundStatus from, RefundStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        assertThat(from.isTerminal()).isEqualTo(REFUND.get(from).isEmpty());
    }
}
