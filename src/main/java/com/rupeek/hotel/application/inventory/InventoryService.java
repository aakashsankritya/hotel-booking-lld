package com.rupeek.hotel.application.inventory;

import com.rupeek.hotel.application.common.KeyedLocks;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.model.RoomInventory;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.repository.InventoryRepository;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.InventoryKey;
import com.rupeek.hotel.domain.value.RoomTypeId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Holds and releases one unit of a room type for every night of a stay.
 * All changes to the inventory of one room type run under that room type's lock.
 * Lock order in this application: booking lock first, then room type lock.
 */
@Service
public class InventoryService {

    private final InventoryRepository inventories;
    private final KeyedLocks<RoomTypeId> roomTypeLocks = new KeyedLocks<>();

    public InventoryService(InventoryRepository inventories) {
        this.inventories = inventories;
    }

    /**
     * All or nothing: if one night has no free unit, no night is held.
     *
     * @throws InsufficientInventoryException if any night is full
     */
    public void hold(RoomType roomType, DateRange stay) {
        roomTypeLocks.runWithLock(roomType.id(), () -> {
            List<RoomInventory> nights = stay.dates().stream()
                    .map(date -> inventories.getOrOpen(new InventoryKey(roomType.id(), date), roomType.totalUnits()))
                    .toList();
            nights.forEach(night -> {
                if (!night.canHold(1)) {
                    throw new InsufficientInventoryException(
                            roomType.id(), night.key().date(), 1, night.available());
                }
            });
            nights.forEach(night -> night.hold(1));
        });
    }

    /** @throws IllegalStateException if a night was never held (a bug, not a user error) */
    public void release(RoomTypeId roomTypeId, DateRange stay) {
        roomTypeLocks.runWithLock(roomTypeId, () -> {
            for (LocalDate date : stay.dates()) {
                inventories.find(new InventoryKey(roomTypeId, date))
                        .orElseThrow(() -> new IllegalStateException(
                                "No inventory to release for " + roomTypeId + " on " + date))
                        .release(1);
            }
        });
    }
}
