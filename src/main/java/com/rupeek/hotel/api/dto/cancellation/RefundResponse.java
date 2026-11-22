package com.rupeek.hotel.api.dto.cancellation;

import com.rupeek.hotel.api.dto.MoneyDto;
import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.value.RefundQuote;

/**
 * {@code status} is {@code SUCCEEDED} or {@code FAILED} (the gateway answer), or {@code NOT_APPLICABLE}
 * when the policy gives 0% and so no refund was sent. {@code failureReason} is set only for {@code FAILED}.
 */
public record RefundResponse(String status, MoneyDto amount, MoneyDto cancellationFee, String reason,
        String failureReason) {

    static final String NOT_APPLICABLE = "NOT_APPLICABLE";

    public static RefundResponse from(RefundQuote quote, GatewayResult refund) {
        MoneyDto amount = MoneyDto.from(quote.refundAmount());
        MoneyDto fee = MoneyDto.from(quote.cancellationFee());
        if (refund == null) {
            return new RefundResponse(NOT_APPLICABLE, amount, fee, quote.reason(), null);
        }
        return new RefundResponse(refund.success() ? "SUCCEEDED" : "FAILED", amount, fee, quote.reason(),
                refund.failureReason());
    }
}
