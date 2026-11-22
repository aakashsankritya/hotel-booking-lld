package com.rupeek.hotel.api.dto.cancellation;

import com.rupeek.hotel.api.dto.booking.BookingResponse;
import com.rupeek.hotel.application.cancellation.CancellationOutcome;

/** {@code refund} is null when the booking was never paid. */
public record CancellationResponse(BookingResponse booking, RefundResponse refund) {

    public static CancellationResponse from(CancellationOutcome outcome) {
        RefundResponse refund = outcome.quote() == null ? null : RefundResponse.from(outcome.quote(), outcome.refund());
        return new CancellationResponse(BookingResponse.from(outcome.booking()), refund);
    }
}
