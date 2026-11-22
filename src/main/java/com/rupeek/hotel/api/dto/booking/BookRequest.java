package com.rupeek.hotel.api.dto.booking;

import com.rupeek.hotel.application.booking.BookCommand;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.GuestDetails;
import com.rupeek.hotel.domain.value.PropertyId;
import com.rupeek.hotel.domain.value.RoomTypeId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookRequest(
        @NotBlank String propertyId,
        @NotBlank String roomTypeId,
        @NotBlank String guestName,
        @NotBlank String guestEmail,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotNull Integer guests) {

    /** An id that is not a UUID makes the domain throw, which is a 400. */
    public BookCommand toCommand() {
        return new BookCommand(PropertyId.of(propertyId), RoomTypeId.of(roomTypeId),
                new GuestDetails(guestName, guestEmail), new DateRange(checkIn, checkOut), new GuestCount(guests));
    }
}
