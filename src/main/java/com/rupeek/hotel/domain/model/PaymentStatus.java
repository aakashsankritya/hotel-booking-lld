package com.rupeek.hotel.domain.model;

import com.rupeek.hotel.domain.common.TransitionTable;

/**
 * Payment life cycle.
 *
 * <pre>
 * INITIATED → SUCCEEDED, FAILED
 * SUCCEEDED → REFUNDED
 * FAILED, REFUNDED → (terminal)
 * </pre>
 */
public enum PaymentStatus {
    INITIATED, SUCCEEDED, FAILED, REFUNDED;

    /** Shared with {@link Payment}, which uses it to check every move. */
    static final TransitionTable<PaymentStatus> TABLE = TransitionTable.of(PaymentStatus.class)
            .allow(INITIATED, SUCCEEDED, FAILED)
            .allow(SUCCEEDED, REFUNDED)
            .build();
}
