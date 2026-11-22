package com.rupeek.hotel.api.dto.property;

import com.rupeek.hotel.domain.search.SearchCriteria;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.PriceRange;
import com.rupeek.hotel.domain.value.StarRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of {@code GET /properties/search}. Prices are INR per night.
 * {@code amenities} is a comma list, for example {@code amenities=WIFI,POOL}.
 */
public record SearchRequest(
        @NotBlank String city,
        String locality,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        @NotNull Integer guests,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minStars,
        Set<Amenity> amenities) {

    private static final String CURRENCY = "INR";

    public SearchCriteria toCriteria() {
        return new SearchCriteria(city, locality, new DateRange(checkIn, checkOut), new GuestCount(guests),
                new PriceRange(inr(minPrice), inr(maxPrice)), minStars == null ? null : new StarRating(minStars),
                amenities);
    }

    private static Money inr(BigDecimal price) {
        return price == null ? null : Money.of(price.toPlainString(), CURRENCY);
    }
}
