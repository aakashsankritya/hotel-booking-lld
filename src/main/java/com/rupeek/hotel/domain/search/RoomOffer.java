package com.rupeek.hotel.domain.search;

import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.value.Money;

/** A room type the guest can book, with the total price for the whole stay. */
public record RoomOffer(RoomType roomType, Money totalPrice) {
}
