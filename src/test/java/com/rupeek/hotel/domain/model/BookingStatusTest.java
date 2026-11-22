package com.rupeek.hotel.domain.model;

import static com.rupeek.hotel.domain.model.BookingStatus.CANCELLED;
import static com.rupeek.hotel.domain.model.BookingStatus.CONFIRMED;
import static com.rupeek.hotel.domain.model.BookingStatus.EXPIRED;
import static com.rupeek.hotel.domain.model.BookingStatus.PAYMENT_FAILED;
import static com.rupeek.hotel.domain.model.BookingStatus.PENDING_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BookingStatusTest {

    /** The table from the spec, written again here so the test does not copy the code under test. */
    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED = Map.of(
            PENDING_PAYMENT, Set.of(CONFIRMED, PAYMENT_FAILED, EXPIRED, CANCELLED),
            CONFIRMED, Set.of(CANCELLED),
            PAYMENT_FAILED, Set.of(),
            EXPIRED, Set.of(),
            CANCELLED, Set.of());

    static Stream<Arguments> everyPair() {
        return Stream.of(BookingStatus.values()).flatMap(from -> Stream.of(BookingStatus.values())
                .map(to -> Arguments.of(from, to, ALLOWED.get(from).contains(to))));
    }

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @MethodSource("everyPair")
    void theTableMatchesTheSpec(BookingStatus from, BookingStatus to, boolean allowed) {
        assertThat(BookingStatus.TABLE.allows(from, to)).isEqualTo(allowed);
    }

    @Test
    void theTableCoversAllTwentyFivePairs() {
        assertThat(everyPair().count()).isEqualTo(25);
    }
}
