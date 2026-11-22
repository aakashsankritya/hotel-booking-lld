package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocationTest {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void blankCityIsRejected(String city) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new Location(city, "Indiranagar"));
    }

    @Test
    void nullCityIsRejected() {
        assertThatNullPointerException().isThrownBy(() -> new Location(null, "Indiranagar"));
    }

    @Test
    void fieldsAreStoredTrimmedAndLowerCased() {
        Location location = new Location("  Bengaluru ", " Indiranagar  ");

        assertThat(location.city()).isEqualTo("bengaluru");
        assertThat(location.locality()).isEqualTo("indiranagar");
        assertThat(location).isEqualTo(new Location("BENGALURU", "INDIRANAGAR"));
    }

    @Test
    void localityIsOptional() {
        assertThat(new Location("Pune", null).locality()).isEmpty();
        assertThat(new Location("Pune", "  ").locality()).isEmpty();
    }

    @Test
    void matchesCityIgnoresCaseAndSpaces() {
        Location location = new Location("Bengaluru", "Indiranagar");

        assertThat(location.matchesCity(" BENGALURU ")).isTrue();
        assertThat(location.matchesCity("Mumbai")).isFalse();
    }

    @Test
    void matchesLocalityIgnoresCaseAndSpaces() {
        Location location = new Location("Bengaluru", "Indiranagar");

        assertThat(location.matchesLocality("indiranagar")).isTrue();
        assertThat(location.matchesLocality("Koramangala")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void blankCriterionMatchesEverything(String criterion) {
        Location location = new Location("Bengaluru", "Indiranagar");

        assertThat(location.matchesCity(criterion)).isTrue();
        assertThat(location.matchesLocality(criterion)).isTrue();
        assertThat(location.matchesCity(null)).isTrue();
        assertThat(location.matchesLocality(null)).isTrue();
    }

    @Test
    void localityCriterionDoesNotMatchLocationWithoutLocality() {
        assertThat(new Location("Pune", null).matchesLocality("Baner")).isFalse();
    }
}
