package com.rupeek.hotel.api.dto.booking;

import com.rupeek.hotel.api.dto.MoneyDto;
import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.model.BookingStatus;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(String id, BookingStatus status, String propertyId, String roomTypeId,
        String guestName, LocalDate checkIn, LocalDate checkOut, int nights, int guests, MoneyDto amount,
        Instant holdExpiresAt, Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.id().value().toString(), booking.status(),
                booking.propertyId().value().toString(), booking.roomTypeId().value().toString(),
                booking.guest().name(), booking.stay().checkIn(), booking.stay().checkOut(),
                booking.stay().nights(), booking.guests().value(), MoneyDto.from(booking.amount()),
                booking.holdExpiresAt(), booking.createdAt());
    }
}
