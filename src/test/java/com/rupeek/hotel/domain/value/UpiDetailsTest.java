package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UpiDetailsTest {

    @ParameterizedTest
    @ValueSource(strings = {"akash@okhdfc", "ab@ab", "a.b-c_d9@upi", "9876543210@ybl"})
    void validVpaIsAccepted(String vpa) {
        assertThat(new UpiDetails(vpa).vpa()).isEqualTo(vpa);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "akash", "@upi", "a@upi", "akash@u", "akash@ok1", "akash@", "ak ash@upi",
            "akash@upi ", "akash@upi\n", "akash@@upi", "ak$h@upi"})
    void invalidVpaIsRejected(String vpa) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new UpiDetails(vpa));
    }

    @Test
    void nullVpaIsRejected() {
        assertThatNullPointerException().isThrownBy(() -> new UpiDetails(null));
    }

    @Test
    void typeIsUpi() {
        assertThat(new UpiDetails("akash@upi").type()).isEqualTo(PaymentMethodType.UPI);
    }
}
