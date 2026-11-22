package com.rupeek.hotel.api.dto.owner;

import com.rupeek.hotel.domain.model.Owner;
import com.rupeek.hotel.domain.value.PropertyId;
import java.util.List;

public record OwnerResponse(String id, String name, String email, List<String> propertyIds, boolean multiProperty) {

    public static OwnerResponse from(Owner owner) {
        List<String> propertyIds = owner.propertyIds().stream().map(PropertyId::value).map(Object::toString).toList();
        return new OwnerResponse(owner.id().value().toString(), owner.name(), owner.email(), propertyIds,
                owner.isMultiProperty());
    }
}
