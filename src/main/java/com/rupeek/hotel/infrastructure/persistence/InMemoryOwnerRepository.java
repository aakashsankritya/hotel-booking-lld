package com.rupeek.hotel.infrastructure.persistence;

import com.rupeek.hotel.domain.model.Owner;
import com.rupeek.hotel.domain.repository.OwnerRepository;
import com.rupeek.hotel.domain.value.OwnerId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOwnerRepository implements OwnerRepository {

    private final Map<OwnerId, Owner> store = new ConcurrentHashMap<>();

    @Override
    public void save(Owner owner) {
        store.put(owner.id(), owner);
    }

    @Override
    public Optional<Owner> findById(OwnerId id) {
        return Optional.ofNullable(store.get(id));
    }
}
