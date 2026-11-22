package com.rupeek.hotel.domain.model;

import com.rupeek.hotel.domain.common.Validations;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RoomTypeId;
import java.util.Objects;

/**
 * A kind of room in a property, for example "Deluxe King". It lives inside the {@link Property}
 * aggregate and is immutable: to change a room type, replace it. Identity is the id only.
 */
public final class RoomType {

    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 10;

    private final RoomTypeId id;
    private final String name;
    private final int capacity;
    private final int totalUnits;
    private final Money basePricePerNight;

    private RoomType(RoomTypeId id, String name, int capacity, int totalUnits, Money basePricePerNight) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.totalUnits = totalUnits;
        this.basePricePerNight = basePricePerNight;
    }

    public static RoomType create(String name, int capacity, int totalUnits, Money basePrice) {
        Objects.requireNonNull(basePrice, "basePrice must not be null");
        String cleanName = Validations.requireNotBlank(name, "room type name");
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new DomainValidationException(
                    "capacity must be between " + MIN_CAPACITY + " and " + MAX_CAPACITY + ": " + capacity);
        }
        if (totalUnits < 1) {
            throw new DomainValidationException("totalUnits must be at least 1: " + totalUnits);
        }
        if (basePrice.isZero()) {
            throw new DomainValidationException("base price per night must be greater than zero");
        }
        return new RoomType(RoomTypeId.newId(), cleanName, capacity, totalUnits, basePrice);
    }

    public boolean canHost(GuestCount guests) {
        return guests.fitsIn(capacity);
    }

    public RoomTypeId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int capacity() {
        return capacity;
    }

    public int totalUnits() {
        return totalUnits;
    }

    public Money basePricePerNight() {
        return basePricePerNight;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof RoomType other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RoomType[" + id + ", " + name + "]";
    }
}
