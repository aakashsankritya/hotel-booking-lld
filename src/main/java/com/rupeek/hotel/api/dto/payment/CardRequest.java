package com.rupeek.hotel.api.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;

/** Only the last 4 digits. The API never accepts a full card number or a CVV. */
public record CardRequest(@NotBlank String last4, @NotNull YearMonth expiry, @NotBlank String holderName) {
}
