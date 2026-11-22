package com.rupeek.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.value.PropertyId;
import org.junit.jupiter.api.Test;

class OwnerTest {

    private final Owner owner = Owner.register("Asha Rao", "asha@example.com");

    @Test
    void registerValidatesNameAndEmail() {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Owner.register(" ", "a@b.com"));
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> Owner.register("Asha", "nope"));
        assertThat(owner.propertyIds()).isEmpty();
    }

    @Test
    void duplicatePropertyThrows() {
        PropertyId property = PropertyId.newId();
        owner.addProperty(property);

        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> owner.addProperty(property));
        assertThat(owner.propertyIds()).containsExactly(property);
    }

    @Test
    void isMultiPropertyIsFalseForOneAndTrueForTwo() {
        assertThat(owner.isMultiProperty()).isFalse();

        owner.addProperty(PropertyId.newId());
        assertThat(owner.isMultiProperty()).isFalse();

        owner.addProperty(PropertyId.newId());
        assertThat(owner.isMultiProperty()).isTrue();
    }

    @Test
    void propertyIdsIsAnUnmodifiableCopyInInsertionOrder() {
        PropertyId first = PropertyId.newId();
        PropertyId second = PropertyId.newId();
        owner.addProperty(first);
        owner.addProperty(second);

        assertThat(owner.propertyIds()).containsExactly(first, second);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> owner.propertyIds().add(PropertyId.newId()));
    }

    @Test
    void equalityUsesIdOnly() {
        Owner same = Owner.register("Asha Rao", "asha@example.com");

        assertThat(owner).isNotEqualTo(same).isEqualTo(owner).hasSameHashCodeAs(owner);
    }
}
