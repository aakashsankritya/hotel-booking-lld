package com.rupeek.hotel.domain.search.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.search.SearchCandidate;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.PriceRange;
import com.rupeek.hotel.domain.value.StarRating;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchFiltersTest {

    private static final DateRange STAY = new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));

    private final RoomType room = RoomType.create("Deluxe", 3, 5, Money.of("3000", "INR"));
    private final Property property = Property.onboard(OwnerId.newId(), "Sea View", new Location("Goa", "Baga"),
            new StarRating(4), Set.of(Amenity.WIFI, Amenity.POOL), CancellationPolicyType.FLEXIBLE, List.of(room));
    private final SearchCandidate candidate = new SearchCandidate(property, room);

    private static SearchCriteria criteria(String city, String locality, int guests, PriceRange price,
            StarRating stars, Set<Amenity> amenities) {
        return new SearchCriteria(city, locality, STAY, new GuestCount(guests), price, stars, amenities);
    }

    private static SearchCriteria any() {
        return criteria("Goa", null, 2, null, null, null);
    }

    @Test
    void locationFilterMatchesCityIgnoringCase() {
        assertThat(new LocationFilter().accepts(candidate, criteria("GOA", null, 2, null, null, null))).isTrue();
        assertThat(new LocationFilter().accepts(candidate, criteria("Pune", null, 2, null, null, null))).isFalse();
    }

    @Test
    void locationFilterChecksLocalityOnlyWhenGiven() {
        assertThat(new LocationFilter().accepts(candidate, criteria("Goa", "baga", 2, null, null, null))).isTrue();
        assertThat(new LocationFilter().accepts(candidate, criteria("Goa", "Calangute", 2, null, null, null))).isFalse();
        assertThat(new LocationFilter().accepts(candidate, criteria("Goa", " ", 2, null, null, null))).isTrue();
    }

    @Test
    void capacityFilterRejectsTooManyGuests() {
        assertThat(new CapacityFilter().accepts(candidate, criteria("Goa", null, 3, null, null, null))).isTrue();
        assertThat(new CapacityFilter().accepts(candidate, criteria("Goa", null, 4, null, null, null))).isFalse();
    }

    @Test
    void priceFilterUsesInclusiveBoundsOnPricePerNight() {
        PriceRange exact = new PriceRange(Money.of("3000", "INR"), Money.of("3000", "INR"));
        PriceRange below = new PriceRange(null, Money.of("2999", "INR"));
        PriceRange above = new PriceRange(Money.of("3001", "INR"), null);

        assertThat(new PriceFilter().accepts(candidate, criteria("Goa", null, 2, exact, null, null))).isTrue();
        assertThat(new PriceFilter().accepts(candidate, criteria("Goa", null, 2, below, null, null))).isFalse();
        assertThat(new PriceFilter().accepts(candidate, criteria("Goa", null, 2, above, null, null))).isFalse();
        assertThat(new PriceFilter().accepts(candidate, any())).isTrue();
    }

    @Test
    void starRatingFilterIsAMinimum() {
        assertThat(new StarRatingFilter().accepts(candidate, criteria("Goa", null, 2, null, new StarRating(4), null))).isTrue();
        assertThat(new StarRatingFilter().accepts(candidate, criteria("Goa", null, 2, null, new StarRating(5), null))).isFalse();
        assertThat(new StarRatingFilter().accepts(candidate, any())).isTrue();
    }

    @Test
    void amenityFilterNeedsEveryRequestedAmenity() {
        assertThat(new AmenityFilter().accepts(candidate,
                criteria("Goa", null, 2, null, null, Set.of(Amenity.WIFI, Amenity.POOL)))).isTrue();
        assertThat(new AmenityFilter().accepts(candidate,
                criteria("Goa", null, 2, null, null, Set.of(Amenity.WIFI, Amenity.SPA)))).isFalse();
        assertThat(new AmenityFilter().accepts(candidate, any())).isTrue();
    }
}
