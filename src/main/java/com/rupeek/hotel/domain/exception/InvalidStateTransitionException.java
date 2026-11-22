package com.rupeek.hotel.domain.exception;

/** The entity is in a state that does not allow the requested change. HTTP 409. */
public class InvalidStateTransitionException extends DomainException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String entity;
    private final transient Object id;
    private final transient Enum<?> from;
    private final transient Enum<?> to;

    public InvalidStateTransitionException(String entity, Object id, Enum<?> from, Enum<?> to) {
        super(entity + " " + id + ": cannot change state from " + from + " to " + to);
        this.entity = entity;
        this.id = id;
        this.from = from;
        this.to = to;
    }

    /** Use this form when the state table allows the move but another rule blocks it. */
    public InvalidStateTransitionException(String entity, Object id, Enum<?> from, Enum<?> to, String reason) {
        super(entity + " " + id + ": cannot change state from " + from + " to " + to + " (" + reason + ")");
        this.entity = entity;
        this.id = id;
        this.from = from;
        this.to = to;
    }

    public String entity() {
        return entity;
    }

    public Object id() {
        return id;
    }

    public Enum<?> from() {
        return from;
    }

    public Enum<?> to() {
        return to;
    }
}
