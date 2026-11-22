package com.rupeek.hotel.domain.search.filter;

import com.rupeek.hotel.domain.repository.InventoryRepository;
import com.rupeek.hotel.domain.search.SearchCandidate;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.search.SearchFilter;
import com.rupeek.hotel.domain.value.InventoryKey;
import java.util.Objects;

/**
 * Keeps a room type only if at least one unit is free on every night of the stay.
 * A night with no inventory record has no bookings yet, so all units are free.
 * Search only reads. It never opens an inventory.
 * Snapshot read, no lock; the binding check happens at booking time.
 */
public final class AvailabilityFilter implements SearchFilter {

    private final InventoryRepository inventories;

    public AvailabilityFilter(InventoryRepository inventories) {
        this.inventories = Objects.requireNonNull(inventories, "inventories");
    }

    @Override
    public boolean accepts(SearchCandidate candidate, SearchCriteria criteria) {
        return criteria.stay().dates().stream().allMatch(date ->
                inventories.find(new InventoryKey(candidate.roomType().id(), date))
                        .map(inventory -> inventory.canHold(1))
                        .orElse(true));
    }
}
