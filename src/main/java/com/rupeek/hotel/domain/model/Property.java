package com.rupeek.hotel.domain.model;

import com.rupeek.hotel.domain.common.Validations;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.PropertyId;
import com.rupeek.hotel.domain.value.RoomTypeId;
import com.rupeek.hotel.domain.value.StarRating;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A hotel property. This is an aggregate root: it owns its {@link RoomType}s, and room types are
 * reached only through it. The owner-property link is stored on both sides. The property service
 * adds the property id to the {@link Owner} in the same use case.
 *
 * <p>Not thread-safe. The repository or service that shares a property between threads must guard it.
 */
public final class Property {

    private final PropertyId id;
    private final OwnerId ownerId;
    private final String name;
    private final Location location;
    private final StarRating starRating;
    private final EnumSet<Amenity> amenities;
    private final Map<RoomTypeId, RoomType> roomTypes = new LinkedHashMap<>();
    private final CancellationPolicyType cancellationPolicyType;

    private Property(PropertyId id, OwnerId ownerId, String name, Location location, StarRating starRating,
            EnumSet<Amenity> amenities, CancellationPolicyType cancellationPolicyType) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.location = location;
        this.starRating = starRating;
        this.amenities = amenities;
        this.cancellationPolicyType = cancellationPolicyType;
    }

    /** Creates a property. It needs at least one room type, and room type names are unique (ignoring case). */
    public static Property onboard(OwnerId ownerId, String name, Location location, StarRating starRating,
            Set<Amenity> amenities, CancellationPolicyType cancellationPolicyType, List<RoomType> roomTypes) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(starRating, "starRating must not be null");
        Objects.requireNonNull(amenities, "amenities must not be null");
        Objects.requireNonNull(cancellationPolicyType, "cancellationPolicyType must not be null");
        Objects.requireNonNull(roomTypes, "roomTypes must not be null");
        String cleanName = Validations.requireNotBlank(name, "property name");
        if (roomTypes.isEmpty()) {
            throw new DomainValidationException("a property needs at least one room type");
        }
        EnumSet<Amenity> amenityCopy = EnumSet.noneOf(Amenity.class);
        amenityCopy.addAll(amenities);

        Property property = new Property(PropertyId.newId(), ownerId, cleanName, location, starRating, amenityCopy,
                cancellationPolicyType);
        roomTypes.forEach(property::addRoomType);
        return property;
    }

    /** Adds a room type. A room type with the same name (ignoring case) already in the property is an error. */
    public void addRoomType(RoomType roomType) {
        Objects.requireNonNull(roomType, "roomType must not be null");
        boolean duplicate = roomTypes.containsKey(roomType.id())
                || roomTypes.values().stream().anyMatch(existing -> existing.name().equalsIgnoreCase(roomType.name()));
        if (duplicate) {
            throw new DomainValidationException("room type already exists in this property: " + roomType.name());
        }
        roomTypes.put(roomType.id(), roomType);
    }

    public RoomType roomType(RoomTypeId roomTypeId) {
        RoomType roomType = roomTypes.get(roomTypeId);
        if (roomType == null) {
            throw new EntityNotFoundException("RoomType", roomTypeId);
        }
        return roomType;
    }

    public List<RoomType> roomTypes() {
        return List.copyOf(roomTypes.values());
    }

    public boolean hasAllAmenities(Set<Amenity> required) {
        return amenities.containsAll(required);
    }

    public PropertyId id() {
        return id;
    }

    public OwnerId ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public Location location() {
        return location;
    }

    public StarRating starRating() {
        return starRating;
    }

    public Set<Amenity> amenities() {
        return Set.copyOf(amenities);
    }

    public CancellationPolicyType cancellationPolicyType() {
        return cancellationPolicyType;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Property other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Property[" + id + ", " + name + "]";
    }
}
