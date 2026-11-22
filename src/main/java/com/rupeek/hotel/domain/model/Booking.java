package com.rupeek.hotel.domain.model;

import static com.rupeek.hotel.domain.model.BookingStatus.TABLE;

import com.rupeek.hotel.domain.exception.BookingHoldExpiredException;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.GuestDetails;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PropertyId;
import com.rupeek.hotel.domain.value.RoomTypeId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A guest's booking of one room type for a stay. Aggregate root.
 *
 * <p>The booking starts as {@code PENDING_PAYMENT} with a hold that ends at {@code holdExpiresAt}.
 * Every state change first passes the {@link BookingStatus} transition table, then
 * {@link #transitionTo} updates {@code status}, {@code updatedAt} and {@code version}.
 *
 * <p>Time comes from the {@link Clock} passed to each method. The clock zone decides which calendar
 * day "today" is for the check-in rule in {@link #cancel(Clock)}. Configure the app clock with the
 * property's zone (for example Asia/Kolkata).
 *
 * <p>Not thread-safe. The booking service serialises access to one booking.
 */
public final class Booking {

    private static final String ENTITY = "Booking";

    private final BookingId id;
    private final PropertyId propertyId;
    private final RoomTypeId roomTypeId;
    private final GuestDetails guest;
    private final DateRange stay;
    private final GuestCount guests;
    private final Money amount;
    private final Instant createdAt;
    private final Instant holdExpiresAt;
    private BookingStatus status;
    private Instant updatedAt;
    private Instant cancelledAt;
    /** Placeholder for optimistic locking. It is used when a real database replaces the in-memory repositories. */
    private long version;

    private Booking(BookingId id, PropertyId propertyId, RoomTypeId roomTypeId, GuestDetails guest, DateRange stay,
            GuestCount guests, Money amount, Instant now, Instant holdExpiresAt) {
        this.id = id;
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.guest = guest;
        this.stay = stay;
        this.guests = guests;
        this.amount = amount;
        this.createdAt = now;
        this.updatedAt = now;
        this.holdExpiresAt = holdExpiresAt;
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    /** Creates a booking in {@code PENDING_PAYMENT}. The hold ends at {@code now + holdTtl}. */
    public static Booking hold(PropertyId propertyId, RoomTypeId roomTypeId, GuestDetails guest, DateRange stay,
            GuestCount guests, Money amount, Clock clock, Duration holdTtl) {
        Objects.requireNonNull(propertyId, "propertyId must not be null");
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        Objects.requireNonNull(guest, "guest must not be null");
        Objects.requireNonNull(stay, "stay must not be null");
        Objects.requireNonNull(guests, "guests must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(holdTtl, "holdTtl must not be null");
        if (holdTtl.isZero() || holdTtl.isNegative()) {
            throw new DomainValidationException("holdTtl must be greater than zero: " + holdTtl);
        }
        Instant now = clock.instant();
        return new Booking(BookingId.newId(), propertyId, roomTypeId, guest, stay, guests, amount, now,
                now.plus(holdTtl));
    }

    /**
     * {@code PENDING_PAYMENT} → {@code CONFIRMED}.
     *
     * @throws BookingHoldExpiredException if the hold ended at or before now
     * @throws InvalidStateTransitionException if the booking is not {@code PENDING_PAYMENT}
     */
    public void confirm(Clock clock) {
        Instant now = clock.instant();
        BookingStatus next = TABLE.requireTransition(ENTITY, id, status, BookingStatus.CONFIRMED);
        if (isHoldExpired(now)) { // after the state check, so a confirmed booking is not reported as "expired"
            throw new BookingHoldExpiredException(id, holdExpiresAt);
        }
        transitionTo(next, now);
    }

    /** {@code PENDING_PAYMENT} → {@code PAYMENT_FAILED}. */
    public void markPaymentFailed(Clock clock) {
        transitionTo(TABLE.requireTransition(ENTITY, id, status, BookingStatus.PAYMENT_FAILED), clock.instant());
    }

    /**
     * {@code PENDING_PAYMENT} → {@code EXPIRED}. Allowed only when {@code now >= holdExpiresAt}.
     *
     * @throws InvalidStateTransitionException if the booking is not {@code PENDING_PAYMENT}, or the hold is still valid
     */
    public void expire(Clock clock) {
        Instant now = clock.instant();
        BookingStatus next = TABLE.requireTransition(ENTITY, id, status, BookingStatus.EXPIRED);
        if (!isHoldExpired(now)) {
            throw new InvalidStateTransitionException(ENTITY, id, status, next,
                    "hold is valid until " + holdExpiresAt);
        }
        transitionTo(next, now);
    }

    /**
     * Cancels a {@code PENDING_PAYMENT} or {@code CONFIRMED} booking. Check-in must be after today.
     *
     * @return the status before the cancel, so the service knows if a refund is needed
     * @throws InvalidStateTransitionException if the booking cannot be cancelled in its state,
     *         or check-in is today or in the past
     */
    public BookingStatus cancel(Clock clock) {
        Instant now = clock.instant();
        BookingStatus next = TABLE.requireTransition(ENTITY, id, status, BookingStatus.CANCELLED);
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        if (!today.isBefore(stay.checkIn())) {
            throw new InvalidStateTransitionException(ENTITY, id, status, next,
                    "check-in " + stay.checkIn() + " is not after today " + today);
        }
        BookingStatus previous = status;
        transitionTo(next, now);
        cancelledAt = now;
        return previous;
    }

    /** True if the hold ended: {@code now >= holdExpiresAt}. */
    public boolean isHoldExpired(Instant now) {
        return !now.isBefore(holdExpiresAt);
    }

    /** Applies a move that {@code TABLE.requireTransition} already approved. */
    private void transitionTo(BookingStatus next, Instant now) {
        status = next;
        updatedAt = now;
        version++;
    }

    public BookingId id() {
        return id;
    }

    public PropertyId propertyId() {
        return propertyId;
    }

    public RoomTypeId roomTypeId() {
        return roomTypeId;
    }

    public GuestDetails guest() {
        return guest;
    }

    public DateRange stay() {
        return stay;
    }

    public GuestCount guests() {
        return guests;
    }

    public Money amount() {
        return amount;
    }

    public BookingStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant holdExpiresAt() {
        return holdExpiresAt;
    }

    /** Null until the booking is cancelled. */
    public Instant cancelledAt() {
        return cancelledAt;
    }

    /** Starts at 0. Each state change adds 1. */
    public long version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Booking other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Booking[" + id + ", " + status + "]";
    }
}
