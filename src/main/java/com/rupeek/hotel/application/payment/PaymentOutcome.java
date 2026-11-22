package com.rupeek.hotel.application.payment;

import com.rupeek.hotel.domain.model.BookingStatus;
import com.rupeek.hotel.domain.model.Payment;

public record PaymentOutcome(Payment payment, BookingStatus bookingStatus) {
}
