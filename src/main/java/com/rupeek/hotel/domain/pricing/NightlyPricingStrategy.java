package com.rupeek.hotel.domain.pricing;

import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.Money;

/** Base price per night times the number of nights. */
public final class NightlyPricingStrategy implements PricingStrategy {

    @Override
    public Money priceFor(RoomType roomType, DateRange stay) {
        return roomType.basePricePerNight().times(stay.nights());
    }
}
