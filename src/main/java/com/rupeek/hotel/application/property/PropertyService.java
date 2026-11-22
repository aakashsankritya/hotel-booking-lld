package com.rupeek.hotel.application.property;

import com.rupeek.hotel.application.common.KeyedLocks;
import com.rupeek.hotel.domain.model.Owner;
import com.rupeek.hotel.domain.model.Property;
import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.repository.OwnerRepository;
import com.rupeek.hotel.domain.repository.PropertyRepository;
import com.rupeek.hotel.domain.value.OwnerId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

    private final OwnerRepository owners;
    private final PropertyRepository properties;
    private final KeyedLocks<OwnerId> ownerLocks = new KeyedLocks<>();

    public PropertyService(OwnerRepository owners, PropertyRepository properties) {
        this.owners = owners;
        this.properties = properties;
    }

    public Owner registerOwner(String name, String email) {
        Owner owner = Owner.register(name, email);
        owners.save(owner);
        return owner;
    }

    /** Read only, so no lock. An unknown id throws {@code EntityNotFoundException}. */
    public Owner getOwner(OwnerId id) {
        return owners.getById(id);
    }

    /** Runs under the owner lock, so two onboardings of one owner cannot lose each other's property id. */
    public Property onboard(OnboardPropertyCommand command) {
        return ownerLocks.withLock(command.ownerId(), () -> {
            Owner owner = owners.getById(command.ownerId());
            List<RoomType> roomTypes = command.roomTypes().stream()
                    .map(spec -> RoomType.create(spec.name(), spec.capacity(), spec.units(), spec.price()))
                    .toList();
            Property property = Property.onboard(owner.id(), command.name(), command.location(), command.stars(),
                    command.amenities(), command.policyType(), roomTypes);
            properties.save(property);
            owner.addProperty(property.id());
            owners.save(owner);
            return property;
        });
    }
}
