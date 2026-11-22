package com.rupeek.hotel.infrastructure.persistence;

import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.repository.PropertyRepository;
import com.rupeek.hotel.domain.value.PropertyId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {

    private final Map<PropertyId, Property> store = new ConcurrentHashMap<>();

    @Override
    public void save(Property property) {
        store.put(property.id(), property);
    }

    @Override
    public Optional<Property> findById(PropertyId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Property> findByCity(String city) {
        return store.values().stream().filter(property -> property.location().matchesCity(city)).toList();
    }
}
