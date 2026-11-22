package com.rupeek.hotel.application.cancellation;

import com.rupeek.hotel.domain.model.Booking;
import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.value.RefundQuote;

/**
 * {@code quote} and {@code refund} are null if the booking was never paid.
 * {@code refund} is the gateway answer. It is also null for a 0% quote, because no refund was sent.
 */
public record CancellationOutcome(Booking booking, RefundQuote quote, GatewayResult refund) {
}
