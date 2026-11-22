package com.rupeek.hotel.api.dto.property;

import com.rupeek.hotel.api.dto.MoneyDto;
import com.rupeek.hotel.application.property.OnboardPropertyCommand.RoomTypeSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomTypeRequest(
        @NotBlank String name,
        @NotNull Integer capacity,
        @NotNull Integer units,
        @NotNull @Valid MoneyDto pricePerNight) {

    RoomTypeSpec toSpec() {
        return new RoomTypeSpec(name, capacity, units, pricePerNight.toMoney());
    }
}
