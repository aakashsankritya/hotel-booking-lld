package com.rupeek.hotel.application.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.application.Fixture;
import com.rupeek.hotel.application.property.OnboardPropertyCommand.RoomTypeSpec;
import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import com.rupeek.hotel.domain.value.Location;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.StarRating;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class PropertyServiceTest {

    private final Fixture f = new Fixture(CancellationPolicyType.FLEXIBLE, 1);

    private OnboardPropertyCommand command(OwnerId ownerId, String name) {
        return new OnboardPropertyCommand(ownerId, name, new Location("Goa", "Baga"), new StarRating(3), Set.of(),
                CancellationPolicyType.FLEXIBLE, List.of(new RoomTypeSpec("Std", 2, 3, Money.of("1000", "INR"))));
    }

    @Test
    void onboardSavesThePropertyAndLinksItToTheOwner() {
        var property = f.propertyService.onboard(command(f.owner.id(), "Second Inn"));

        assertThat(f.properties.findById(property.id())).containsSame(property);
        assertThat(f.owners.getById(f.owner.id()).propertyIds()).contains(property.id());
        assertThat(f.owner.isMultiProperty()).isTrue(); // the fixture already onboarded one property
    }

    @Test
    void onboardForAnUnknownOwnerThrowsNotFound() {
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> f.propertyService.onboard(command(OwnerId.newId(), "Ghost Inn")));
    }

    @Test
    void parallelOnboardingsOfOneOwnerLoseNoProperty() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String name = "Inn " + i;
                results.add(executor.submit(() -> f.propertyService.onboard(command(f.owner.id(), name))));
            }
            for (Future<?> result : results) {
                result.get();
            }
        }

        assertThat(f.owners.getById(f.owner.id()).propertyIds()).hasSize(21); // 20 new + the fixture property
    }
}
