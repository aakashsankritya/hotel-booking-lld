package com.rupeek.hotel.api.dto.owner;

import jakarta.validation.constraints.NotBlank;

public record RegisterOwnerRequest(@NotBlank String name, @NotBlank String email) {
}
