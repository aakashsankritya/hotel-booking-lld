package com.rupeek.hotel.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.rupeek.hotel.application.Fixture;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.policy.CancellationPolicyType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class BookingConcurrencyTest {

    private static final int THREADS = 50;

    @Test
    void oneUnitAndFiftyGuestsExactlyOneBookingWins() throws Exception {
        Fixture f = new Fixture(CancellationPolicyType.FLEXIBLE, 1);
        BookCommand command = f.bookCommand(Fixture.stay(10, 13));
        int booked = 0;
        int rejected = 0;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(executor.submit(() -> {
                    try {
                        return f.bookingService.book(command);
                    } catch (InsufficientInventoryException e) {
                        return e;
                    }
                }));
            }
            for (Future<Object> result : results) {
                if (result.get() instanceof InsufficientInventoryException) {
                    rejected++;
                } else {
                    booked++;
                }
            }
        }

        assertThat(booked).isEqualTo(1);
        assertThat(rejected).isEqualTo(THREADS - 1);
        for (int day = 10; day <= 12; day++) {
            assertThat(f.held(LocalDate.of(2026, 12, day))).isEqualTo(1);
        }
    }

    @Test
    void fiftyGuestsAndTenUnitsExactlyTenBookingsWin() throws Exception {
        Fixture f = new Fixture(CancellationPolicyType.FLEXIBLE, 10);
        BookCommand command = f.bookCommand(Fixture.stay(10, 12));
        int booked = 0;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(executor.submit(() -> {
                    try {
                        f.bookingService.book(command);
                        return true;
                    } catch (InsufficientInventoryException e) {
                        return false;
                    }
                }));
            }
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    booked++;
                }
            }
        }

        assertThat(booked).isEqualTo(10);
        assertThat(f.held(LocalDate.of(2026, 12, 10))).isEqualTo(10);
        assertThat(f.held(LocalDate.of(2026, 12, 11))).isEqualTo(10);
    }
}
