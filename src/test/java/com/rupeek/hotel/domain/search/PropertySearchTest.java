package com.rupeek.hotel.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.pricing.NightlyPricingStrategy;
import com.rupeek.hotel.domain.search.filter.AmenityFilter;
import com.rupeek.hotel.domain.search.filter.AvailabilityFilter;
import com.rupeek.hotel.domain.search.filter.CapacityFilter;
import com.rupeek.hotel.domain.search.filter.LocationFilter;
import com.rupeek.hotel.domain.search.filter.PriceFilter;
import com.rupeek.hotel.domain.search.filter.StarRatingFilter;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.InventoryKey;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.PriceRange;
import com.rupeek.hotel.domain.value.StarRating;
import com.rupeek.hotel.infrastructure.persistence.InMemoryInventoryRepository;
import com.rupeek.hotel.infrastructure.persistence.InMemoryPropertyRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PropertySearchTest {

    private static final DateRange STAY = new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));

    private final InMemoryPropertyRepository properties = new InMemoryPropertyRepository();
    private final InMemoryInventoryRepository inventories = new InMemoryInventoryRepository();

    private final RoomType seaStandard = RoomType.create("Standard", 2, 2, Money.of("2000", "INR"));
    private final RoomType seaSuite = RoomType.create("Suite", 4, 1, Money.of("6000", "INR"));
    private final Property seaView = Property.onboard(OwnerId.newId(), "Sea View", new Location("Goa", "Baga"),
            new StarRating(4), Set.of(Amenity.WIFI, Amenity.POOL), CancellationPolicyType.MODERATE,
            List.of(seaStandard, seaSuite));
    private final RoomType budgetRoom = RoomType.create("Basic", 2, 3, Money.of("1000", "INR"));
    private final Property budgetInn = Property.onboard(OwnerId.newId(), "Budget Inn", new Location("Goa", "Calangute"),
            new StarRating(2), Set.of(Amenity.WIFI), CancellationPolicyType.NON_REFUNDABLE, List.of(budgetRoom));
    private final Property puneHotel = Property.onboard(OwnerId.newId(), "Pune Grand", new Location("Pune", "Camp"),
            new StarRating(5), Set.of(Amenity.WIFI), CancellationPolicyType.FLEXIBLE,
            List.of(RoomType.create("Std", 2, 2, Money.of("4000", "INR"))));

    private final List<SearchFilter> filters = new ArrayList<>(List.of(
            new LocationFilter(), new CapacityFilter(), new PriceFilter(), new StarRatingFilter(),
            new AmenityFilter(), new AvailabilityFilter(inventories)));
    private final PropertySearch search = new PropertySearch(properties, filters, new NightlyPricingStrategy());

    PropertySearchTest() {
        properties.save(seaView);
        properties.save(budgetInn);
        properties.save(puneHotel);
    }

    private static SearchCriteria goa(int guests, PriceRange price, StarRating stars, Set<Amenity> amenities) {
        return new SearchCriteria("Goa", null, STAY, new GuestCount(guests), price, stars, amenities);
    }

    private static Set<String> names(List<PropertySearchResult> results) {
        return results.stream().map(r -> r.property().name()).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void returnsOnlyPropertiesInTheCity() {
        assertThat(names(search.search(goa(2, null, null, null)))).containsExactlyInAnyOrder("Sea View", "Budget Inn");
    }

    @Test
    void groupsRoomOffersUnderOnePropertyWithTotalStayPrice() {
        PropertySearchResult result = search.search(goa(2, null, null, null)).stream()
                .filter(r -> r.property().name().equals("Sea View")).findFirst().orElseThrow();

        assertThat(result.offers()).extracting(RoomOffer::totalPrice)
                .containsExactlyInAnyOrder(Money.of("4000", "INR"), Money.of("12000", "INR"));
    }

    @Test
    void roomTypeThatFailsAFilterIsDroppedButPropertyStaysWithOtherRooms() {
        List<PropertySearchResult> results = search.search(goa(2, new PriceRange(null, Money.of("3000", "INR")), null, null));

        PropertySearchResult sea = results.stream().filter(r -> r.property().name().equals("Sea View")).findFirst().orElseThrow();
        assertThat(sea.offers()).extracting(o -> o.roomType().name()).containsExactly("Standard");
    }

    @Test
    void propertyWithNoMatchingRoomIsDropped() {
        assertThat(names(search.search(goa(2, null, new StarRating(3), null)))).containsExactly("Sea View");
        assertThat(names(search.search(goa(2, null, null, Set.of(Amenity.POOL))))).containsExactly("Sea View");
        assertThat(search.search(goa(2, null, new StarRating(5), null))).isEmpty();
    }

    @Test
    void guestCountFiltersSmallRooms() {
        List<PropertySearchResult> results = search.search(goa(4, null, null, null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).offers()).extracting(o -> o.roomType().name()).containsExactly("Suite");
    }

    @Test
    void fullyBookedRoomIsNotOffered() {
        for (var date : STAY.dates()) {
            inventories.getOrOpen(new InventoryKey(seaSuite.id(), date), 1).hold(1);
        }

        List<PropertySearchResult> results = search.search(goa(4, null, null, null));

        assertThat(results).isEmpty();
    }

    @Test
    void localityNarrowsTheSearch() {
        SearchCriteria criteria = new SearchCriteria("Goa", "Calangute", STAY, new GuestCount(2), null, null, null);

        assertThat(names(search.search(criteria))).containsExactly("Budget Inn");
    }

    @Test
    void unknownCityReturnsNothing() {
        SearchCriteria criteria = new SearchCriteria("Mumbai", null, STAY, new GuestCount(2), null, null, null);

        assertThat(search.search(criteria)).isEmpty();
    }

    @Test
    void newRuleNeedsNoChangeInPropertySearch() {
        SearchFilter noSuites = (candidate, criteria) -> !candidate.roomType().name().equals("Suite");
        PropertySearch custom = new PropertySearch(properties, List.of(new LocationFilter(), noSuites),
                new NightlyPricingStrategy());

        PropertySearchResult sea = custom.search(goa(2, null, null, null)).stream()
                .filter(r -> r.property().name().equals("Sea View")).findFirst().orElseThrow();
        assertThat(sea.offers()).extracting(o -> o.roomType().name()).containsExactly("Standard");
    }

    @Test
    void criteriaAppliesDefaultsForOptionalFields() {
        SearchCriteria criteria = new SearchCriteria(" Goa ", null, STAY, new GuestCount(2), null, null, null);

        assertThat(criteria.city()).isEqualTo("Goa");
        assertThat(criteria.priceRange()).isEqualTo(new PriceRange(null, null));
        assertThat(criteria.amenities()).isEmpty();
    }
}
