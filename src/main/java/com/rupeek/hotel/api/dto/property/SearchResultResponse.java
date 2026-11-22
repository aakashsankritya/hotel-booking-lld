package com.rupeek.hotel.api.dto.property;

import com.rupeek.hotel.domain.search.PropertySearchResult;
import com.rupeek.hotel.domain.value.Amenity;
import java.util.List;
import java.util.Set;

public record SearchResultResponse(String propertyId, String name, String city, String locality, int stars,
        Set<Amenity> amenities, List<OfferResponse> offers) {

    public static SearchResultResponse from(PropertySearchResult result) {
        var property = result.property();
        return new SearchResultResponse(property.id().value().toString(), property.name(),
                property.location().city(), property.location().locality(), property.starRating().value(),
                property.amenities(), result.offers().stream().map(OfferResponse::from).toList());
    }
}
