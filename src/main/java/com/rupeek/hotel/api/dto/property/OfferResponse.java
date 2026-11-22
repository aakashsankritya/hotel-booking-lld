package com.rupeek.hotel.api.dto.property;

import com.rupeek.hotel.api.dto.MoneyDto;
import com.rupeek.hotel.domain.search.RoomOffer;

public record OfferResponse(String roomTypeId, String name, int capacity, MoneyDto pricePerNight,
        MoneyDto totalPrice) {

    public static OfferResponse from(RoomOffer offer) {
        var room = offer.roomType();
        return new OfferResponse(room.id().value().toString(), room.name(), room.capacity(),
                MoneyDto.from(room.basePricePerNight()), MoneyDto.from(offer.totalPrice()));
    }
}
