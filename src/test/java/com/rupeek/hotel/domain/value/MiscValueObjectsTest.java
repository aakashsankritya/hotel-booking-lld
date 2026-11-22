package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Small value objects that need only a few checks each. */
class MiscValueObjectsTest {

    @Nested
    class IdempotencyKeyTest {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        void blankIsRejected(String value) {
            assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new IdempotencyKey(value));
        }

        @Test
        void nullIsRejected() {
            assertThatNullPointerException().isThrownBy(() -> new IdempotencyKey(null));
        }

        @Test
        void maxLengthIs64() {
            assertThat(new IdempotencyKey("k".repeat(64)).value()).hasSize(64);
            assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new IdempotencyKey("k".repeat(65)));
        }
    }

    @Nested
    class InventoryKeyTest {

        @Test
        void worksAsMapKey() {
            RoomTypeId room = RoomTypeId.newId();
            Map<InventoryKey, Integer> map = new HashMap<>();
            map.put(new InventoryKey(room, LocalDate.of(2027, 1, 1)), 5);

            assertThat(map.get(new InventoryKey(room, LocalDate.of(2027, 1, 1)))).isEqualTo(5);
            assertThat(map.get(new InventoryKey(room, LocalDate.of(2027, 1, 2)))).isNull();
            assertThat(map.get(new InventoryKey(RoomTypeId.newId(), LocalDate.of(2027, 1, 1)))).isNull();
        }

        @Test
        void nullsAreRejected() {
            assertThatNullPointerException().isThrownBy(() -> new InventoryKey(null, LocalDate.now()));
            assertThatNullPointerException().isThrownBy(() -> new InventoryKey(RoomTypeId.newId(), null));
        }
    }

    @Nested
    class WalletDetailsTest {

        @Test
        void blankFieldsAreRejected() {
            assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new WalletDetails(" ", "w-1"));
            assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new WalletDetails("paytm", ""));
        }

        @Test
        void typeIsWallet() {
            assertThat(new WalletDetails("paytm", "w-1").type()).isEqualTo(PaymentMethodType.WALLET);
        }
    }

    @Nested
    class CancellationTierTest {

        @Test
        void negativeNoticeIsRejected() {
            assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> new CancellationTier(Duration.ofHours(-1), 50));
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 101})
        void percentOutsideZeroToHundredIsRejected(int percent) {
            assertThatExceptionOfType(DomainValidationException.class)
                    .isThrownBy(() -> new CancellationTier(Duration.ofHours(24), percent));
        }

        @Test
        void boundariesAreAccepted() {
            assertThat(new CancellationTier(Duration.ZERO, 0).refundPercent()).isZero();
            assertThat(new CancellationTier(Duration.ofDays(7), 100).refundPercent()).isEqualTo(100);
        }
    }
}
