package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.common.Validations;

/**
 * Contact details of the guest who books. There is no user model, because auth is out of scope.
 * Both fields are stored trimmed.
 */
public record GuestDetails(String name, String email) {

    public GuestDetails {
        name = Validations.requireNotBlank(name, "name");
        email = Validations.requireValidEmail(email);
    }
}
