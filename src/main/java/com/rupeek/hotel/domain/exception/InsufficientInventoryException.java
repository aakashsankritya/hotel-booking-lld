package com.rupeek.hotel.domain.exception;

import com.rupeek.hotel.domain.value.RoomTypeId;
import java.time.LocalDate;

/** Not enough free units of a room type on a date. HTTP 409. */
public class InsufficientInventoryException extends DomainException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final transient RoomTypeId roomTypeId;
    private final LocalDate date;
    private final int requested;
    private final int available;

    public InsufficientInventoryException(RoomTypeId roomTypeId, LocalDate date, int requested, int available) {
        super("Room type " + roomTypeId + " on " + date + ": requested " + requested + ", available " + available);
        this.roomTypeId = roomTypeId;
        this.date = date;
        this.requested = requested;
        this.available = available;
    }

    public RoomTypeId roomTypeId() {
        return roomTypeId;
    }

    public LocalDate date() {
        return date;
    }

    public int requested() {
        return requested;
    }

    public int available() {
        return available;
    }
}
