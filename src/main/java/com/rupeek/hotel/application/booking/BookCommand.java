package com.rupeek.hotel.application.booking;

import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.GuestCount;
import com.rupeek.hotel.domain.value.GuestDetails;
import com.rupeek.hotel.domain.value.PropertyId;
import com.rupeek.hotel.domain.value.RoomTypeId;

public record BookCommand(PropertyId propertyId, RoomTypeId roomTypeId, GuestDetails guest, DateRange stay,
        GuestCount guests) {
}
