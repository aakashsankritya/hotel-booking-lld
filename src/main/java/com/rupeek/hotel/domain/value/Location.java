package com.rupeek.hotel.domain.value;

import com.rupeek.hotel.domain.common.Validations;
import java.util.Objects;

/**
 * City and locality of a property.
 *
 * <p>Both fields are stored trimmed, in the case the owner typed them, so responses show the display
 * form. Matching ignores case and surrounding spaces ({@link #matchesCity}, {@link #matchesLocality}).
 * The locality is optional and is stored as an empty string when it is missing.
 */
public record Location(String city, String locality) {

    public Location {
        Objects.requireNonNull(city, "city must not be null");
        city = Validations.requireNotBlank(city, "city").strip();
        locality = locality == null ? "" : locality.strip();
    }

    /** A blank criterion matches every location. */
    public boolean matchesCity(String criterion) {
        return criterion == null || criterion.isBlank() || city.equalsIgnoreCase(criterion.strip());
    }

    /** A blank criterion matches every location. */
    public boolean matchesLocality(String criterion) {
        return criterion == null || criterion.isBlank() || locality.equalsIgnoreCase(criterion.strip());
    }
}
