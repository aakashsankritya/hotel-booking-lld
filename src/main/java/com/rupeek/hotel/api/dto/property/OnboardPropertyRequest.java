package com.rupeek.hotel.api.dto.property;

import com.rupeek.hotel.application.property.OnboardPropertyCommand;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.StarRating;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

/** Bean Validation checks only that a field is present. The domain checks ranges and rules. */
public record OnboardPropertyRequest(
        @NotBlank String name,
        @NotBlank String city,
        String locality,
        @NotNull Integer stars,
        Set<Amenity> amenities,
        @NotNull CancellationPolicyType cancellationPolicy,
        @NotEmpty @Valid List<RoomTypeRequest> roomTypes) {

    public OnboardPropertyCommand toCommand(OwnerId ownerId) {
        return new OnboardPropertyCommand(ownerId, name, new Location(city, locality), new StarRating(stars),
                amenities == null ? Set.of() : amenities, cancellationPolicy,
                roomTypes.stream().map(RoomTypeRequest::toSpec).toList());
    }
}
