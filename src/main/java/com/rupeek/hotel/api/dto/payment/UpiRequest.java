package com.rupeek.hotel.api.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record UpiRequest(@NotBlank String vpa) {
}
