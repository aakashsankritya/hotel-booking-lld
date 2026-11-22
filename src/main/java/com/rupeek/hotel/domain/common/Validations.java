package com.rupeek.hotel.domain.common;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.regex.Pattern;

/** Small checks shared by value objects and entities. */
public final class Validations {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validations() {
    }

    /** Returns the trimmed text. A null or blank value is a validation error. */
    public static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " must not be blank");
        }
        return value.trim();
    }

    /** Returns the trimmed email. The check is simple on purpose: text@text.text, no spaces. */
    public static String requireValidEmail(String value) {
        String email = requireNotBlank(value, "email");
        if (!EMAIL.matcher(email).matches()) {
            throw new DomainValidationException("invalid email: " + email);
        }
        return email;
    }
}
