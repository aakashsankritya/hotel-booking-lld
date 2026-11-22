package com.rupeek.hotel.domain.repository;

import com.rupeek.hotel.domain.model.RoomInventory;
import com.rupeek.hotel.domain.value.InventoryKey;
import java.util.Optional;

public interface InventoryRepository {

    /**
     * Returns the inventory for the key. If there is none, it opens one with {@code totalUnits}.
     * This call is atomic: all callers for one key get the same instance.
     */
    RoomInventory getOrOpen(InventoryKey key, int totalUnits);

    /** Read only. Empty if nobody has booked that room type on that night yet. */
    Optional<RoomInventory> find(InventoryKey key);
}
