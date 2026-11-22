package com.rupeek.hotel.domain.exception;

/** One generic "not found" error for all entity types. HTTP 404. */
public class EntityNotFoundException extends DomainException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String type;
    private final transient Object id;

    public EntityNotFoundException(String type, Object id) {
        super(type + " not found: " + id);
        this.type = type;
        this.id = id;
    }

    public String type() {
        return type;
    }

    public Object id() {
        return id;
    }
}
