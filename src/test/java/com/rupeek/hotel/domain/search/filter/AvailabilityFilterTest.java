package com.rupeek.hotel.domain.search.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.search.SearchCandidate;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.InventoryKey;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.StarRating;
import com.rupeek.hotel.infrastructure.persistence.InMemoryInventoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AvailabilityFilterTest {

    private static final LocalDate NIGHT_1 = LocalDate.of(2026, 12, 10);
    private static final LocalDate NIGHT_2 = LocalDate.of(2026, 12, 11);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 12);

    private final InMemoryInventoryRepository inventories = new InMemoryInventoryRepository();
    private final AvailabilityFilter filter = new AvailabilityFilter(inventories);
    private final RoomType room = RoomType.create("Deluxe", 2, 1, Money.of("3000", "INR"));
    private final Property property = Property.onboard(OwnerId.newId(), "Sea View", new Location("Goa", "Baga"),
            new StarRating(4), Set.of(), CancellationPolicyType.FLEXIBLE, List.of(room));
    private final SearchCandidate candidate = new SearchCandidate(property, room);
    private final SearchCriteria criteria = new SearchCriteria("Goa", null,
            new DateRange(NIGHT_1, CHECK_OUT), new GuestCount(2), null, null, null);

    private InventoryKey key(LocalDate date) {
        return new InventoryKey(room.id(), date);
    }

    @Test
    void roomWithNoInventoryRecordIsAvailable() {
        assertThat(filter.accepts(candidate, criteria)).isTrue();
    }

    @Test
    void roomIsDroppedWhenAnyNightIsFull() {
        inventories.getOrOpen(key(NIGHT_2), 1).hold(1);

        assertThat(filter.accepts(candidate, criteria)).isFalse();
    }

    @Test
    void roomIsKeptWhenEveryNightStillHasAFreeUnit() {
        inventories.getOrOpen(key(NIGHT_1), 2).hold(1);
        inventories.getOrOpen(key(NIGHT_2), 2).hold(1);

        assertThat(filter.accepts(candidate, criteria)).isTrue();
    }

    @Test
    void checkOutNightIsNotChecked() {
        inventories.getOrOpen(key(CHECK_OUT), 1).hold(1);

        assertThat(filter.accepts(candidate, criteria)).isTrue();
    }

    @Test
    void searchNeverOpensInventory() {
        filter.accepts(candidate, criteria);

        assertThat(inventories.find(key(NIGHT_1))).isEmpty();
    }
}
