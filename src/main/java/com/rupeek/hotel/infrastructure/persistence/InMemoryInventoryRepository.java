package com.rupeek.hotel.infrastructure.persistence;

import com.rupeek.hotel.domain.model.RoomInventory;
import com.rupeek.hotel.domain.repository.InventoryRepository;
import com.rupeek.hotel.domain.value.InventoryKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<InventoryKey, RoomInventory> store = new ConcurrentHashMap<>();

    @Override
    public RoomInventory getOrOpen(InventoryKey key, int totalUnits) {
        return store.computeIfAbsent(key, k -> RoomInventory.open(k, totalUnits)); // atomic
    }

    @Override
    public Optional<RoomInventory> find(InventoryKey key) {
        return Optional.ofNullable(store.get(key));
    }
}
