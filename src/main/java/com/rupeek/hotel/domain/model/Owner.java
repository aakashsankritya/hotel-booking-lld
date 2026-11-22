package com.rupeek.hotel.domain.model;

import com.rupeek.hotel.domain.common.Validations;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.value.OwnerId;
import com.rupeek.hotel.domain.value.PropertyId;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The owner of one or more properties. Whether the owner has many properties is derived from the
 * set of property ids ({@link #isMultiProperty()}). There is no flag to keep in step. This is the
 * Composite point of the design: one owner, one or many properties, same code path.
 */
public final class Owner {

    private final OwnerId id;
    private final String name;
    private final String email;
    private final LinkedHashSet<PropertyId> propertyIds = new LinkedHashSet<>();

    private Owner(OwnerId id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public static Owner register(String name, String email) {
        return new Owner(OwnerId.newId(), Validations.requireNotBlank(name, "name"),
                Validations.requireValidEmail(email));
    }

    public void addProperty(PropertyId propertyId) {
        Objects.requireNonNull(propertyId, "propertyId must not be null");
        if (!propertyIds.add(propertyId)) {
            throw new DomainValidationException("owner already has property " + propertyId);
        }
    }

    /** An unmodifiable copy, in the order the properties were added. */
    public Set<PropertyId> propertyIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(propertyIds));
    }

    /** Derived from the property ids. It is never stored. */
    public boolean isMultiProperty() {
        return propertyIds.size() > 1;
    }

    public OwnerId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Owner other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Owner[" + id + ", " + name + "]";
    }
}
