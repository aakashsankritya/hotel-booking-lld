package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.Amenity;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.RoomTypeId;
import com.rupeek.hotel.domain.value.StarRating;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PropertyTest {

    private static final Money PRICE = Money.of("3000", "INR");

    private static RoomType room(String name, int capacity) {
        return RoomType.create(name, capacity, 5, PRICE);
    }

    private static Property property(Set<Amenity> amenities, List<RoomType> rooms) {
        return Property.onboard(OwnerId.newId(), "Sea View", new Location("Goa", "Baga"), new StarRating(4),
                amenities, CancellationPolicyType.MODERATE, rooms);
    }

    @Test
    void onboardWithNoRoomTypesThrows() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> property(Set.of(), List.of()));
    }

    @Test
    void blankNameThrows() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Property.onboard(
                OwnerId.newId(), " ", new Location("Goa", null), new StarRating(3), Set.of(),
                CancellationPolicyType.FLEXIBLE, List.of(room("Std", 2))));
    }

    @Test
    void duplicateRoomTypeNameThrowsIgnoringCase() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> property(Set.of(), List.of(room("Deluxe", 2), room("DELUXE", 3))));
    }

    @Test
    void addRoomTypeWithDuplicateNameThrowsAndAddsNothing() {
        Property property = property(Set.of(), List.of(room("Deluxe", 2)));

        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> property.addRoomType(room(" deluxe ", 4)));
        assertThat(property.roomTypes()).hasSize(1);
    }

    @Test
    void addRoomTypeAddsANewName() {
        Property property = property(Set.of(), List.of(room("Deluxe", 2)));
        RoomType suite = room("Suite", 4);

        property.addRoomType(suite);

        assertThat(property.roomTypes()).hasSize(2);
        assertThat(property.roomType(suite.id())).isSameAs(suite);
    }

    @Test
    void roomTypesForFiltersByCapacity() {
        RoomType small = room("Small", 2);
        RoomType medium = room("Medium", 4);
        RoomType large = room("Large", 6);
        Property property = property(Set.of(), List.of(small, medium, large));

        assertThat(property.roomTypesFor(new GuestCount(1))).containsExactly(small, medium, large);
        assertThat(property.roomTypesFor(new GuestCount(4))).containsExactly(medium, large);
        assertThat(property.roomTypesFor(new GuestCount(7))).isEmpty();
    }

    @Test
    void unknownRoomTypeThrowsNotFound() {
        Property property = property(Set.of(), List.of(room("Deluxe", 2)));

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> property.roomType(RoomTypeId.newId()))
                .satisfies(e -> assertThat(e.type()).isEqualTo("RoomType"));
    }

    @Test
    void hasAllAmenities() {
        Property property = property(EnumSet.of(Amenity.WIFI, Amenity.POOL), List.of(room("Deluxe", 2)));

        assertThat(property.hasAllAmenities(Set.of())).isTrue();
        assertThat(property.hasAllAmenities(Set.of(Amenity.WIFI))).isTrue();
        assertThat(property.hasAllAmenities(Set.of(Amenity.WIFI, Amenity.POOL))).isTrue();
        assertThat(property.hasAllAmenities(Set.of(Amenity.WIFI, Amenity.GYM))).isFalse();
    }

    @Test
    void inputCollectionsAreCopiedAndOutputsAreUnmodifiable() {
        EnumSet<Amenity> amenities = EnumSet.of(Amenity.WIFI);
        ArrayList<RoomType> rooms = new ArrayList<>(List.of(room("Deluxe", 2)));
        Property property = property(amenities, rooms);

        amenities.add(Amenity.SPA);
        rooms.add(room("Suite", 4));

        assertThat(property.amenities()).containsExactly(Amenity.WIFI);
        assertThat(property.roomTypes()).hasSize(1);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> property.roomTypes().add(room("X", 1)));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> property.amenities().add(Amenity.GYM));
    }

    @Test
    void equalityUsesIdOnly() {
        Property a = property(Set.of(), List.of(room("Deluxe", 2)));
        Property b = property(Set.of(), List.of(room("Deluxe", 2)));

        assertThat(a).isEqualTo(a).isNotEqualTo(b);
    }
}
