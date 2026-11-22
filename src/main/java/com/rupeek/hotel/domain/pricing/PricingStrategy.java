package com.rupeek.hotel.domain.pricing;

import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.Money;

/** Prices one stay in one room type. Add a new class for seasonal or dynamic prices. */
@FunctionalInterface
public interface PricingStrategy {

    Money priceFor(RoomType roomType, DateRange stay);
}
