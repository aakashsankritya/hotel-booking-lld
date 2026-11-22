package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GuestDetailsTest {

    @Test
    void validDetailsAreStoredTrimmed() {
        GuestDetails guest = new GuestDetails("  Asha Rao ", " asha@example.com ");

        assertThat(guest.name()).isEqualTo("Asha Rao");
        assertThat(guest.email()).isEqualTo("asha@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void blankNameIsRejected(String name) {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> new GuestDetails(name, "asha@example.com"));
    }

    @Test
    void nullNameOrEmailIsRejected() {
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> new GuestDetails(null, "asha@example.com"));
        assertThatExceptionOfType(DomainValidationException.class)
                .isThrownBy(() -> new GuestDetails("Asha", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"asha", "asha@", "@example.com", "asha@example", "as ha@example.com", "a@@b.com", "a@b .com"})
    void invalidEmailIsRejected(String email) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new GuestDetails("Asha", email));
    }
}
