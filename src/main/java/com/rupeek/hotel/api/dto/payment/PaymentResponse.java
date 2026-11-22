package com.rupeek.hotel.api.dto.payment;

import com.rupeek.hotel.api.dto.MoneyDto;
import com.rupeek.hotel.application.payment.PaymentOutcome;
import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.PaymentStatus;
import com.rupeek.hotel.domain.value.PaymentMethodType;
import java.time.Instant;

public record PaymentResponse(String paymentId, String bookingId, PaymentMethodType method, MoneyDto amount,
        PaymentStatus status, String failureReason, BookingStatus bookingStatus, Instant createdAt) {

    public static PaymentResponse from(PaymentOutcome outcome) {
        var payment = outcome.payment();
        return new PaymentResponse(payment.id().value().toString(), payment.bookingId().value().toString(),
                payment.method(), MoneyDto.from(payment.amount()), payment.status(), payment.failureReason(),
                outcome.bookingStatus(), payment.createdAt());
    }
}
