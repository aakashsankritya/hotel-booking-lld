package com.rupeek.hotel.domain.model;

import com.rupeek.hotel.domain.common.TransitionTable;

/**
 * Refund life cycle.
 *
 * <pre>
 * INITIATED → SUCCEEDED, FAILED
 * SUCCEEDED, FAILED → (terminal)
 * </pre>
 */
public enum RefundStatus {
    INITIATED, SUCCEEDED, FAILED;

    /** Shared with {@link Refund}, which uses it to check every move. */
    static final TransitionTable<RefundStatus> TABLE = TransitionTable.of(RefundStatus.class)
            .allow(INITIATED, SUCCEEDED, FAILED)
            .build();

    public boolean canTransitionTo(RefundStatus next) {
        return TABLE.allows(this, next);
    }

    public boolean isTerminal() {
        return TABLE.isTerminal(this);
    }
}
