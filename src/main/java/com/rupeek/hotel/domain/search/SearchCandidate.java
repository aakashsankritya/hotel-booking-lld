package com.rupeek.hotel.domain.search;

import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomType;

/** One room type of one property. Filters keep or drop candidates. */
public record SearchCandidate(Property property, RoomType roomType) {
}
