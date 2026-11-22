package com.rupeek.hotel.api.dto.property;

import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.Amenity;
import java.util.List;
import java.util.Set;

public record PropertyResponse(String id, String ownerId, String name, String city, String locality, int stars,
        Set<Amenity> amenities, CancellationPolicyType cancellationPolicy, List<RoomTypeResponse> roomTypes) {

    public static PropertyResponse from(Property property) {
        return new PropertyResponse(property.id().value().toString(), property.ownerId().value().toString(),
                property.name(), property.location().city(), property.location().locality(),
                property.starRating().value(), property.amenities(), property.cancellationPolicyType(),
                property.roomTypes().stream().map(RoomTypeResponse::from).toList());
    }
}
