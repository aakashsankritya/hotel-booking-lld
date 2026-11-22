package com.rupeek.hotel.domain.search;

import com.rupeek.hotel.domain.model.Property;
import java.util.List;

/** A property that matches the search, with the room types the guest can book. */
public record PropertySearchResult(Property property, List<RoomOffer> offers) {

    public PropertySearchResult {
        offers = List.copyOf(offers);
    }
}
