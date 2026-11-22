package com.rupeek.hotel.api.dto.cancellation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.payment.GatewayResult;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.RefundQuote;
import org.junit.jupiter.api.Test;

class RefundResponseTest {

    private static final RefundQuote HALF = RefundQuote.of(Money.of("6000", "INR"), 50, "half refund");

    @Test
    void noGatewayAnswerMeansNotApplicable() {
        RefundResponse response = RefundResponse.from(RefundQuote.of(Money.of("6000", "INR"), 0, "no refund"), null);

        assertThat(response.status()).isEqualTo(RefundResponse.NOT_APPLICABLE);
        assertThat(response.failureReason()).isNull();
        assertThat(response.amount().amount()).isEqualByComparingTo("0");
        assertThat(response.cancellationFee().amount()).isEqualByComparingTo("6000");
    }

    @Test
    void successfulGatewayAnswerIsSucceeded() {
        RefundResponse response = RefundResponse.from(HALF, GatewayResult.success("rf-1"));

        assertThat(response.status()).isEqualTo("SUCCEEDED");
        assertThat(response.failureReason()).isNull();
        assertThat(response.amount().amount()).isEqualByComparingTo("3000");
        assertThat(response.reason()).isEqualTo("half refund");
    }

    @Test
    void failedGatewayAnswerIsFailedWithItsReason() {
        RefundResponse response = RefundResponse.from(HALF, GatewayResult.failure("gateway error: timeout"));

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.failureReason()).isEqualTo("gateway error: timeout");
    }
}
