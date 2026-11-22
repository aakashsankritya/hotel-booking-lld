package com.rupeek.hotel.domain.model;

import com.rupeek.hotel.domain.common.TransitionTable;

/**
 * Booking life cycle. The transition table is in one place, so a new state is a change in one place
 * (open/closed principle).
 *
 * <pre>
 * PENDING_PAYMENT → CONFIRMED, PAYMENT_FAILED, EXPIRED, CANCELLED
 * CONFIRMED       → CANCELLED
 * PAYMENT_FAILED, EXPIRED, CANCELLED → (terminal)
 * </pre>
 */
public enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, PAYMENT_FAILED, EXPIRED, CANCELLED;

    /** Shared with {@link Booking}, which uses it to check every move. */
    static final TransitionTable<BookingStatus> TABLE = TransitionTable.of(BookingStatus.class)
            .allow(PENDING_PAYMENT, CONFIRMED, PAYMENT_FAILED, EXPIRED, CANCELLED)
            .allow(CONFIRMED, CANCELLED)
            .build();

    public boolean canTransitionTo(BookingStatus next) {
        return TABLE.allows(this, next);
    }

    public boolean isTerminal() {
        return TABLE.isTerminal(this);
    }

    /** True while the booking keeps units of inventory: waiting for payment, or confirmed. */
    public boolean holdsInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
