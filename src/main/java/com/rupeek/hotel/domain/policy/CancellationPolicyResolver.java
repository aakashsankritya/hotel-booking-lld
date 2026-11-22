package com.rupeek.hotel.domain.policy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Maps the policy type a property chose to the rules that apply.
 * The map comes from the caller (configuration), so a new policy type needs no change here.
 */
public final class CancellationPolicyResolver {

    private final Map<CancellationPolicyType, CancellationPolicy> policies;

    /** @throws IllegalArgumentException if a {@link CancellationPolicyType} has no policy in the map */
    public CancellationPolicyResolver(Map<CancellationPolicyType, CancellationPolicy> policies) {
        policies.values().forEach(Objects::requireNonNull);
        Set<CancellationPolicyType> missing = EnumSet.allOf(CancellationPolicyType.class);
        missing.removeAll(policies.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("No cancellation policy configured for " + missing);
        }
        this.policies = new EnumMap<>(policies);
    }

    public CancellationPolicy resolve(CancellationPolicyType type) {
        return policies.get(type); // never null: the constructor checked every type
    }
}
