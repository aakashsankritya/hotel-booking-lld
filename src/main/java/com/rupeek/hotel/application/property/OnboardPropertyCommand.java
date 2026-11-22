package com.rupeek.hotel.application.property;

import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.StarRating;
import java.util.List;
import java.util.Set;

public record OnboardPropertyCommand(OwnerId ownerId, String name, Location location, StarRating stars,
        Set<Amenity> amenities, CancellationPolicyType policyType, List<RoomTypeSpec> roomTypes) {

    public record RoomTypeSpec(String name, int capacity, int units, Money price) {
    }
}
