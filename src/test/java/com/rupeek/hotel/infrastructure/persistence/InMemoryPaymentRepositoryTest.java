package com.rupeek.hotel.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.domain.model.Payment;
import com.rupeek.hotel.domain.value.BookingId;
import com.rupeek.hotel.domain.value.IdempotencyKey;
import com.rupeek.hotel.domain.value.Money;
import com.rupeek.hotel.domain.value.UpiDetails;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class InMemoryPaymentRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-01T10:00:00Z"), ZoneOffset.UTC);

    private final InMemoryPaymentRepository repository = new InMemoryPaymentRepository();

    private static Payment payment(String key) {
        return Payment.initiate(BookingId.newId(), Money.of("5000", "INR"), new UpiDetails("asha@okbank"),
                new IdempotencyKey(key), CLOCK);
    }

    @Test
    void firstSaveIfAbsentSavesAndReturnsEmpty() {
        Payment payment = payment("k1");

        assertThat(repository.saveIfAbsent(payment)).isEmpty();
        assertThat(repository.findByIdempotencyKey(new IdempotencyKey("k1"))).containsSame(payment);
    }

    @Test
    void secondSaveIfAbsentWithSameKeyReturnsTheFirstPayment() {
        Payment first = payment("k1");
        repository.saveIfAbsent(first);

        assertThat(repository.saveIfAbsent(payment("k1"))).containsSame(first);
        assertThat(repository.findByIdempotencyKey(new IdempotencyKey("k1"))).containsSame(first);
    }

    @Test
    void differentKeysAreIndependent() {
        assertThat(repository.saveIfAbsent(payment("k1"))).isEmpty();
        assertThat(repository.saveIfAbsent(payment("k2"))).isEmpty();
    }

    @Test
    void findByIdempotencyKeyIsEmptyForUnknownKey() {
        assertThat(repository.findByIdempotencyKey(new IdempotencyKey("nope"))).isEmpty();
    }

    @Test
    void concurrentRequestsWithOneKeyHaveExactlyOneWinner() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                results.add(executor.submit(() -> repository.saveIfAbsent(payment("same-key")).isEmpty()));
            }
            long winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
        }
    }
}
