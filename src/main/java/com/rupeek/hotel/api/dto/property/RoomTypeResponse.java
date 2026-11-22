package com.rupeek.hotel.api.dto.property;

import com.rupeek.hotel.api.dto.MoneyDto;
import com.rupeek.hotel.domain.model.RoomType;

public record RoomTypeResponse(String id, String name, int capacity, int units, MoneyDto pricePerNight) {

    public static RoomTypeResponse from(RoomType roomType) {
        return new RoomTypeResponse(roomType.id().value().toString(), roomType.name(), roomType.capacity(),
                roomType.totalUnits(), MoneyDto.from(roomType.basePricePerNight()));
    }
}
