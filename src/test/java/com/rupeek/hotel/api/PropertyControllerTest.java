package com.rupeek.hotel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rupeek.hotel.application.property.OnboardPropertyCommand;
import com.rupeek.hotel.application.property.PropertyService;
import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.search.PropertySearch;
import com.rupeek.hotel.domain.search.PropertySearchResult;
import com.rupeek.hotel.domain.search.RoomOffer;
import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.StarRating;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(PropertyController.class)
class PropertyControllerTest {

    private static final String ONBOARD_JSON = """
            {"name": "Sea View", "city": "Goa", "locality": "Baga", "stars": 4, "amenities": ["WIFI", "POOL"],
             "cancellationPolicy": "MODERATE",
             "roomTypes": [{"name": "Deluxe", "capacity": 2, "units": 3,
                            "pricePerNight": {"amount": 3000.00, "currency": "INR"}}]}
            """;
    private static final String SEARCH_BASE = "/properties/search?city=Goa&checkIn=2026-12-10&checkOut=2026-12-12&guests=2";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PropertyService properties;
    @MockitoBean
    private PropertySearch search;

    private final OwnerId ownerId = OwnerId.newId();
    private final RoomType room = RoomType.create("Deluxe", 2, 3, Money.of("3000", "INR"));
    private final Property property = Property.onboard(ownerId, "Sea View", new Location("Goa", "Baga"),
            new StarRating(4), Set.of(Amenity.WIFI, Amenity.POOL), CancellationPolicyType.MODERATE, List.of(room));

    private MvcTestResult onboard(String ownerIdValue, String body) {
        return mvc.post().uri("/owners/{id}/properties", ownerIdValue).contentType(MediaType.APPLICATION_JSON)
                .content(body).exchange();
    }

    @Test
    void onboardReturns201AndPassesTheRequestAsACommand() {
        when(properties.onboard(any(OnboardPropertyCommand.class))).thenReturn(property);

        MvcTestResult result = onboard(ownerId.value().toString(), ONBOARD_JSON);

        assertThat(result).hasStatus(201);
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Sea View");
        assertThat(result).bodyJson().extractingPath("$.roomTypes[0].name").isEqualTo("Deluxe");
        ArgumentCaptor<OnboardPropertyCommand> captor = ArgumentCaptor.forClass(OnboardPropertyCommand.class);
        verify(properties).onboard(captor.capture());
        OnboardPropertyCommand command = captor.getValue();
        assertThat(command.ownerId()).isEqualTo(ownerId);
        assertThat(command.stars()).isEqualTo(new StarRating(4));
        assertThat(command.amenities()).containsExactlyInAnyOrder(Amenity.WIFI, Amenity.POOL);
        assertThat(command.policyType()).isEqualTo(CancellationPolicyType.MODERATE);
        assertThat(command.roomTypes()).singleElement().satisfies(spec -> {
            assertThat(spec.units()).isEqualTo(3);
            assertThat(spec.price()).isEqualTo(Money.of("3000", "INR"));
        });
    }

    @Test
    void onboardWithEmptyRoomTypesReturns400() {
        String json = ONBOARD_JSON.substring(0, ONBOARD_JSON.indexOf("\"roomTypes\"")) + "\"roomTypes\": []}";

        assertThat(onboard(ownerId.value().toString(), json)).hasStatus(400);
        verifyNoInteractions(properties);
    }

    @Test
    void onboardWithAnOwnerIdThatIsNotAUuidReturns400() {
        assertThat(onboard("not-a-uuid", ONBOARD_JSON)).hasStatus(400);
        verifyNoInteractions(properties);
    }

    @Test
    void searchBindsAllParameters() {
        when(search.search(any(SearchCriteria.class))).thenReturn(List.of());

        MvcTestResult result = mvc.get().uri(SEARCH_BASE
                + "&locality=Baga&minPrice=1000&maxPrice=5000&minStars=3&amenities=WIFI,POOL").exchange();

        assertThat(result).hasStatus(200);
        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(search).search(captor.capture());
        SearchCriteria criteria = captor.getValue();
        assertThat(criteria.city()).isEqualTo("Goa");
        assertThat(criteria.locality()).isEqualTo("Baga");
        assertThat(criteria.stay().checkIn()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(criteria.stay().checkOut()).isEqualTo(LocalDate.of(2026, 12, 12));
        assertThat(criteria.guests().value()).isEqualTo(2);
        assertThat(criteria.priceRange().min()).isEqualTo(Money.of("1000", "INR"));
        assertThat(criteria.priceRange().max()).isEqualTo(Money.of("5000", "INR"));
        assertThat(criteria.minStars()).isEqualTo(new StarRating(3));
        assertThat(criteria.amenities()).containsExactlyInAnyOrder(Amenity.WIFI, Amenity.POOL);
    }

    @Test
    void searchWithOnlyTheRequiredParametersLeavesTheRestOpen() {
        when(search.search(any(SearchCriteria.class))).thenReturn(List.of());

        assertThat(mvc.get().uri(SEARCH_BASE).exchange()).hasStatus(200);

        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(search).search(captor.capture());
        SearchCriteria criteria = captor.getValue();
        assertThat(criteria.priceRange().min()).isNull();
        assertThat(criteria.priceRange().max()).isNull();
        assertThat(criteria.minStars()).isNull();
        assertThat(criteria.amenities()).isEmpty();
    }

    @Test
    void searchReturnsThePropertiesWithTheirOffers() {
        when(search.search(any(SearchCriteria.class))).thenReturn(
                List.of(new PropertySearchResult(property, List.of(new RoomOffer(room, Money.of("6000", "INR"))))));

        MvcTestResult result = mvc.get().uri(SEARCH_BASE).exchange();

        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$[0].name").isEqualTo("Sea View");
        assertThat(result).bodyJson().extractingPath("$[0].offers[0].name").isEqualTo("Deluxe");
        assertThat(result).bodyJson().extractingPath("$[0].offers[0].totalPrice.currency").isEqualTo("INR");
    }

    @Test
    void searchWithoutCityReturns400() {
        assertThat(mvc.get().uri("/properties/search?checkIn=2026-12-10&checkOut=2026-12-12&guests=2").exchange())
                .hasStatus(400);
        verifyNoInteractions(search);
    }

    @Test
    void searchWithABadDateReturns400() {
        assertThat(mvc.get().uri("/properties/search?city=Goa&checkIn=10-12-2026&checkOut=2026-12-12&guests=2")
                .exchange()).hasStatus(400);
        verifyNoInteractions(search);
    }

    @Test
    void searchWithAMinPriceAboveTheMaxPriceReturns400FromTheDomain() {
        MvcTestResult result = mvc.get().uri(SEARCH_BASE + "&minPrice=5000&maxPrice=1000").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("DomainValidationException");
        verifyNoInteractions(search);
    }
}
