package com.rupeek.hotel.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.rupeek.hotel.domain.value.CancellationTier;

import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.Money;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CancellationPolicyResolverTest {

    private static final Money PAID = Money.of("10000", "INR");
    private static final DateRange STAY = new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11));
    private static final Instant CHECK_IN = Instant.parse("2026-12-10T12:00:00Z");

    private static CancellationPolicy tiered(CancellationTier... tiers) {
        return new TieredCancellationPolicy(List.of(tiers), ZoneOffset.UTC);
    }

    private static Map<CancellationPolicyType, CancellationPolicy> standardPolicies() {
        Map<CancellationPolicyType, CancellationPolicy> map = new EnumMap<>(CancellationPolicyType.class);
        map.put(CancellationPolicyType.FLEXIBLE, tiered(new CancellationTier(Duration.ofHours(24), 100)));
        map.put(CancellationPolicyType.MODERATE, tiered(
                new CancellationTier(Duration.ofHours(72), 100), new CancellationTier(Duration.ofHours(24), 50)));
        map.put(CancellationPolicyType.NON_REFUNDABLE, tiered());
        return map;
    }

    private final CancellationPolicyResolver resolver = new CancellationPolicyResolver(standardPolicies());

    private Money refund(CancellationPolicyType type, Duration notice) {
        return resolver.resolve(type).quote(PAID, STAY, CHECK_IN.minus(notice)).refundAmount();
    }

    @Test
    void everyPolicyTypeHasAPolicy() {
        for (CancellationPolicyType type : CancellationPolicyType.values()) {
            assertThat(resolver.resolve(type)).isNotNull();
        }
    }

    @Test
    void flexibleRefundsFullUpTo24HoursBefore() {
        assertThat(refund(CancellationPolicyType.FLEXIBLE, Duration.ofHours(24))).isEqualTo(PAID);
        assertThat(refund(CancellationPolicyType.FLEXIBLE, Duration.ofHours(23)).isZero()).isTrue();
    }

    @Test
    void moderateRefundsFullThenHalfThenNothing() {
        assertThat(refund(CancellationPolicyType.MODERATE, Duration.ofHours(72))).isEqualTo(PAID);
        assertThat(refund(CancellationPolicyType.MODERATE, Duration.ofHours(48))).isEqualTo(Money.of("5000", "INR"));
        assertThat(refund(CancellationPolicyType.MODERATE, Duration.ofHours(10)).isZero()).isTrue();
    }

    @Test
    void nonRefundableNeverRefunds() {
        assertThat(refund(CancellationPolicyType.NON_REFUNDABLE, Duration.ofDays(60)).isZero()).isTrue();
    }

    @Test
    void resolverReturnsTheConfiguredPolicyInstance() {
        Map<CancellationPolicyType, CancellationPolicy> map = standardPolicies();

        assertThat(new CancellationPolicyResolver(map).resolve(CancellationPolicyType.MODERATE))
                .isSameAs(map.get(CancellationPolicyType.MODERATE));
    }

    @Test
    void constructorRejectsAMapThatMissesAPolicyType() {
        Map<CancellationPolicyType, CancellationPolicy> map = standardPolicies();
        map.remove(CancellationPolicyType.MODERATE);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CancellationPolicyResolver(map))
                .withMessageContaining("MODERATE");
    }
}
