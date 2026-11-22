package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.common.Validations;
import java.util.Locale;
import java.util.Objects;

/**
 * City and locality of a property.
 *
 * <p>Both fields are stored trimmed and lower-cased, so matching is case-insensitive and
 * {@code equals} ignores case. The original display form is not kept. The locality is optional
 * and is stored as an empty string when it is missing.
 */
public record Location(String city, String locality) {

    public Location {
        Objects.requireNonNull(city, "city must not be null");
        city = normalize(Validations.requireNotBlank(city, "city"));
        locality = locality == null ? "" : normalize(locality);
    }

    /** A blank criterion matches every location. */
    public boolean matchesCity(String criterion) {
        return criterion == null || criterion.isBlank() || city.equals(normalize(criterion));
    }

    /** A blank criterion matches every location. */
    public boolean matchesLocality(String criterion) {
        return criterion == null || criterion.isBlank() || locality.equals(normalize(criterion));
    }

    private static String normalize(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }
}
