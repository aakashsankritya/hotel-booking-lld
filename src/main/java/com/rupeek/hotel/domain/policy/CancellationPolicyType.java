package com.rupeek.hotel.domain.policy;

/** The cancellation policy a property chooses. The policy classes (later) map each type to refund rules. */
public enum CancellationPolicyType {
    FLEXIBLE, MODERATE, NON_REFUNDABLE
}
