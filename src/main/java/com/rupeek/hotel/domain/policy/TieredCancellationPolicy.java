package com.rupeek.hotel.domain.policy;

import com.rupeek.hotel.domain.value.CancellationTier;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RefundQuote;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Refund depends on how long before check-in the guest cancels.
 * The first tier (longest notice first) that the notice satisfies sets the refund percent.
 * If no tier fits, the refund is 0%. A policy with no tiers never refunds.
 */
public final class TieredCancellationPolicy implements CancellationPolicy {

    private static final LocalTime CHECK_IN_TIME = LocalTime.NOON;

    private final List<CancellationTier> tiers;
    private final ZoneId zone;

    public TieredCancellationPolicy(List<CancellationTier> tiers, ZoneId zone) {
        this.tiers = tiers.stream()
                .sorted(Comparator.comparing(CancellationTier::minNoticeBeforeCheckIn).reversed())
                .toList();
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    @Override
    public RefundQuote quote(Money amountPaid, DateRange stay, Instant cancelledAt) {
        Instant checkIn = stay.checkIn().atTime(CHECK_IN_TIME).atZone(zone).toInstant();
        Duration notice = Duration.between(cancelledAt, checkIn);
        return tiers.stream()
                .filter(tier -> notice.compareTo(tier.minNoticeBeforeCheckIn()) >= 0)
                .findFirst()
                .map(tier -> RefundQuote.of(amountPaid, tier.refundPercent(),
                        tier.refundPercent() + "% refund: cancelled at least "
                                + tier.minNoticeBeforeCheckIn().toHours() + "h before check-in"))
                .orElseGet(() -> RefundQuote.of(amountPaid, 0, "No refund: cancelled too close to check-in"));
    }
}
