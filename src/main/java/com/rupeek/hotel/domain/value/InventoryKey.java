package com.rupeek.hotel.domain.value;

import java.time.LocalDate;
import java.util.Objects;

/** Key for one room type on one night. Used as the map key in the inventory repository. */
public record InventoryKey(RoomTypeId roomTypeId, LocalDate date) {

    public InventoryKey {
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        Objects.requireNonNull(date, "date must not be null");
    }
}
