package com.rupeek.hotel.domain.policy;

import com.rupeek.hotel.domain.value.CancellationTier;
import java.time.Duration;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Maps the policy type a property chose to the rules that apply. */
public final class CancellationPolicyResolver {

    private final Map<CancellationPolicyType, CancellationPolicy> policies = new EnumMap<>(CancellationPolicyType.class);

    public CancellationPolicyResolver(ZoneId zone) {
        policies.put(CancellationPolicyType.FLEXIBLE, new TieredCancellationPolicy(
                List.of(new CancellationTier(Duration.ofHours(24), 100)), zone));
        policies.put(CancellationPolicyType.MODERATE, new TieredCancellationPolicy(
                List.of(new CancellationTier(Duration.ofHours(72), 100),
                        new CancellationTier(Duration.ofHours(24), 50)), zone));
        policies.put(CancellationPolicyType.NON_REFUNDABLE, new TieredCancellationPolicy(List.of(), zone));
    }

    public CancellationPolicy resolve(CancellationPolicyType type) {
        CancellationPolicy policy = policies.get(type);
        if (policy == null) {
            throw new IllegalStateException("No cancellation policy configured for " + type);
        }
        return policy;
    }
}
