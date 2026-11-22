package com.rupeek.hotel.api.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record WalletRequest(@NotBlank String provider, @NotBlank String walletId) {
}
