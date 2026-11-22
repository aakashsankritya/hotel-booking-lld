package com.rupeek.hotel.api.dto;

import com.rupeek.hotel.domain.value.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Money in JSON: {@code {"amount": 2500.00, "currency": "INR"}}. Used in requests and responses. */
public record MoneyDto(@NotNull BigDecimal amount, @NotBlank String currency) {

    public static MoneyDto from(Money money) {
        return new MoneyDto(money.amount(), money.currency().getCurrencyCode());
    }

    /** The domain checks the amount and the currency code. */
    public Money toMoney() {
        return Money.of(amount.toPlainString(), currency);
    }
}
