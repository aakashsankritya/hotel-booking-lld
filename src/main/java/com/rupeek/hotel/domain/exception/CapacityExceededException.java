package com.rupeek.hotel.domain.exception;

/** More guests than the room type can host. HTTP 400. */
public class CapacityExceededException extends DomainException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final int guests;
    private final int capacity;

    public CapacityExceededException(int guests, int capacity) {
        super(guests + " guests do not fit in a room type with capacity " + capacity);
        this.guests = guests;
        this.capacity = capacity;
    }

    public int guests() {
        return guests;
    }

    public int capacity() {
        return capacity;
    }
}
