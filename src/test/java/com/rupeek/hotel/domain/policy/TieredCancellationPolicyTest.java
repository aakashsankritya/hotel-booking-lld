package com.rupeek.hotel.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.value.CancellationTier;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RefundQuote;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class TieredCancellationPolicyTest {

    private static final Money PAID = Money.of("10000", "INR");
    private static final DateRange STAY = new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
    /** Check-in is at 12:00 on the first night. The policy zone is UTC. */
    private static final Instant CHECK_IN = Instant.parse("2026-12-10T12:00:00Z");

    private final TieredCancellationPolicy policy = new TieredCancellationPolicy(
            List.of(new CancellationTier(Duration.ofHours(24), 50), new CancellationTier(Duration.ofHours(72), 100)),
            ZoneOffset.UTC);

    private RefundQuote cancelledBefore(Duration notice) {
        return policy.quote(PAID, STAY, CHECK_IN.minus(notice));
    }

    @Test
    void longNoticeGetsTopTierEvenWhenTiersAreGivenInAnyOrder() {
        assertThat(cancelledBefore(Duration.ofHours(100)).refundAmount()).isEqualTo(Money.of("10000", "INR"));
    }

    @Test
    void middleNoticeGetsMiddleTier() {
        RefundQuote quote = cancelledBefore(Duration.ofHours(48));

        assertThat(quote.refundAmount()).isEqualTo(Money.of("5000", "INR"));
        assertThat(quote.cancellationFee()).isEqualTo(Money.of("5000", "INR"));
    }

    @Test
    void noticeExactlyOnTierBoundaryCountsForThatTier() {
        assertThat(cancelledBefore(Duration.ofHours(72)).refundAmount()).isEqualTo(Money.of("10000", "INR"));
        assertThat(cancelledBefore(Duration.ofHours(24)).refundAmount()).isEqualTo(Money.of("5000", "INR"));
    }

    @Test
    void shortNoticeGetsNoRefund() {
        RefundQuote quote = cancelledBefore(Duration.ofHours(23));

        assertThat(quote.refundAmount().isZero()).isTrue();
        assertThat(quote.cancellationFee()).isEqualTo(PAID);
        assertThat(quote.reason()).isNotBlank();
    }

    @Test
    void cancellingAfterCheckInGetsNoRefund() {
        assertThat(policy.quote(PAID, STAY, CHECK_IN.plus(Duration.ofHours(5))).refundAmount().isZero()).isTrue();
    }

    @Test
    void policyWithNoTiersNeverRefunds() {
        var none = new TieredCancellationPolicy(List.of(), ZoneOffset.UTC);

        assertThat(none.quote(PAID, STAY, CHECK_IN.minus(Duration.ofDays(30))).refundAmount().isZero()).isTrue();
    }

    @Test
    void checkInTimeUsesThePolicyZone() {
        // 12:00 in +05:30 is 06:30 UTC. 30 hours before that is 2026-12-09T00:30Z.
        var india = new TieredCancellationPolicy(List.of(new CancellationTier(Duration.ofHours(30), 100)),
                ZoneOffset.ofHoursMinutes(5, 30));

        assertThat(india.quote(PAID, STAY, Instant.parse("2026-12-09T00:30:00Z")).refundAmount()).isEqualTo(PAID);
        assertThat(india.quote(PAID, STAY, Instant.parse("2026-12-09T00:31:00Z")).refundAmount().isZero()).isTrue();
    }
}
