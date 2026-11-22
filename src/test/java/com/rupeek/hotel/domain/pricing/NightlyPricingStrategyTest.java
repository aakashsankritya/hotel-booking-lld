package com.rupeek.hotel.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.model.RoomType;
import com.rupeek.hotel.domain.value.DateRange;
import com.rupeek.hotel.domain.value.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NightlyPricingStrategyTest {

    private final PricingStrategy pricing = new NightlyPricingStrategy();
    private final RoomType room = RoomType.create("Deluxe", 2, 5, Money.of("3000.50", "INR"));

    @Test
    void priceIsBasePriceTimesNights() {
        DateRange stay = new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13));

        assertThat(pricing.priceFor(room, stay)).isEqualTo(Money.of("9001.50", "INR"));
    }

    @Test
    void oneNightCostsBasePrice() {
        DateRange stay = new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11));

        assertThat(pricing.priceFor(room, stay)).isEqualTo(room.basePricePerNight());
    }
}
