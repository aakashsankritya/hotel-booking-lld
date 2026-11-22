package com.rupeek.hotel.domain.repository;

import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.model.Owner;
import com.rupeek.hotel.domain.value.OwnerId;
import java.util.Optional;

public interface OwnerRepository {

    void save(Owner owner);

    Optional<Owner> findById(OwnerId id);

    default Owner getById(OwnerId id) {
        return findById(id).orElseThrow(() -> new EntityNotFoundException("Owner", id));
    }
}
