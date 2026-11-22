package com.rupeek.hotel.domain.repository;

import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.value.PropertyId;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository {

    void save(Property property);

    Optional<Property> findById(PropertyId id);

    /** Properties in the city. The match ignores case and spaces. */
    List<Property> findByCity(String city);

    default Property getById(PropertyId id) {
        return findById(id).orElseThrow(() -> new EntityNotFoundException("Property", id));
    }
}
