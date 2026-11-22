package com.rupeek.hotel.domain.search;

import com.rupeek.hotel.domain.common.Validations;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.PriceRange;
import com.rupeek.hotel.domain.value.StarRating;
import java.util.Objects;
import java.util.Set;

/**
 * What the guest asks for. City, stay and guests are required.
 * The other fields are optional: {@code null} (or an empty set) means "any".
 * The price range is a price per night.
 */
public record SearchCriteria(
        String city,
        String locality,
        DateRange stay,
        GuestCount guests,
        PriceRange priceRange,
        StarRating minStars,
        Set<Amenity> amenities) {

    public SearchCriteria {
        city = Validations.requireNotBlank(city, "city");
        Objects.requireNonNull(stay, "stay must not be null");
        Objects.requireNonNull(guests, "guests must not be null");
        priceRange = priceRange == null ? new PriceRange(null, null) : priceRange;
        amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
    }
}
