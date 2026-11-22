package com.rupeek.hotel.domain.model;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.value.InventoryKey;
import java.util.Objects;

/**
 * Units of one room type on one night. Identity is the {@link InventoryKey}.
 *
 * <p><b>This class is not thread-safe, by design.</b> It holds plain fields and does no locking.
 * {@code InventoryService} owns the lock: one lock for each room type, which covers every night of
 * that room type. It calls {@link #hold(int)} and {@link #release(int)} only while it holds that lock.
 * Putting the lock here would not help, because a stay covers many nights and the check and the
 * update must be atomic across all of them.
 */
public final class RoomInventory {

    private final InventoryKey key;
    private final int totalUnits;
    private int heldUnits;

    private RoomInventory(InventoryKey key, int totalUnits) {
        this.key = key;
        this.totalUnits = totalUnits;
    }

    public static RoomInventory open(InventoryKey key, int totalUnits) {
        Objects.requireNonNull(key, "key must not be null");
        if (totalUnits < 0) {
            throw new DomainValidationException("totalUnits must not be negative: " + totalUnits);
        }
        return new RoomInventory(key, totalUnits);
    }

    public int available() {
        return totalUnits - heldUnits;
    }

    /** True if {@code units} is positive and not more than {@link #available()}. */
    public boolean canHold(int units) {
        return units > 0 && units <= available();
    }

    /**
     * Holds units.
     *
     * @throws DomainValidationException if {@code units} is not positive
     * @throws InsufficientInventoryException if {@code units} is more than {@link #available()}
     */
    public void hold(int units) {
        requirePositive(units);
        if (units > available()) {
            throw new InsufficientInventoryException(key.roomTypeId(), key.date(), units, available());
        }
        heldUnits += units;
    }

    /**
     * Releases units held before.
     *
     * @throws DomainValidationException if {@code units} is not positive
     * @throws IllegalStateException if {@code units} is more than the held units. That is a bug in
     *         the caller, not a user error.
     */
    public void release(int units) {
        requirePositive(units);
        if (units > heldUnits) {
            throw new IllegalStateException(
                    "cannot release " + units + " units for " + key + ": only " + heldUnits + " held");
        }
        heldUnits -= units;
    }

    public InventoryKey key() {
        return key;
    }

    public int totalUnits() {
        return totalUnits;
    }

    public int heldUnits() {
        return heldUnits;
    }

    private static void requirePositive(int units) {
        if (units <= 0) {
            throw new DomainValidationException("units must be greater than zero: " + units);
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof RoomInventory other && key.equals(other.key));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "RoomInventory[" + key + ", held " + heldUnits + "/" + totalUnits + "]";
    }
}
