package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaymentStatusTest {

    /** The table from the spec, written again here so the test does not copy the code under test. */
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            PaymentStatus.INITIATED, Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED),
            PaymentStatus.SUCCEEDED, Set.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, Set.of(),
            PaymentStatus.REFUNDED, Set.of());

    static Stream<Arguments> everyPair() {
        return Stream.of(PaymentStatus.values()).flatMap(from -> Stream.of(PaymentStatus.values())
                .map(to -> Arguments.of(from, to, ALLOWED.get(from).contains(to))));
    }

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @MethodSource("everyPair")
    void theTableMatchesTheSpec(PaymentStatus from, PaymentStatus to, boolean allowed) {
        assertThat(PaymentStatus.TABLE.allows(from, to)).isEqualTo(allowed);
    }
}
