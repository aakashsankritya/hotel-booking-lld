package com.rupeek.hotel.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.rupeek.hotel.domain.exception.DomainValidationException;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IdsTest {

    static Stream<Arguments> ids() {
        return Stream.of(
                id("OwnerId", OwnerId::of, OwnerId::newId),
                id("PropertyId", PropertyId::of, PropertyId::newId),
                id("RoomTypeId", RoomTypeId::of, RoomTypeId::newId),
                id("BookingId", BookingId::of, BookingId::newId),
                id("PaymentId", PaymentId::of, PaymentId::newId));
    }

    private static Arguments id(String name, Function<String, Object> of, Supplier<Object> newId) {
        return Arguments.of(name, of, newId);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ids")
    void roundTripsThroughString(String name, Function<String, Object> of, Supplier<Object> newId) {
        Object id = newId.get();

        assertThat(of.apply(id.toString())).isEqualTo(id);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ids")
    void newIdIsUnique(String name, Function<String, Object> of, Supplier<Object> newId) {
        assertThat(newId.get()).isNotEqualTo(newId.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ids")
    void invalidUuidIsRejected(String name, Function<String, Object> of, Supplier<Object> newId) {
        assertThatExceptionOfType(DomainValidationException.class).isThrownBy(() -> of.apply("not-a-uuid"));
        assertThatNullPointerException().isThrownBy(() -> of.apply(null));
    }

    @Test
    void toStringIsThePlainUuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(new BookingId(uuid)).hasToString(uuid.toString());
    }

    @Test
    void nullValueIsRejected() {
        assertThatNullPointerException().isThrownBy(() -> new BookingId(null));
    }

    @Test
    void idsOfDifferentTypesAreNotEqual() {
        UUID uuid = UUID.randomUUID();

        assertThat((Object) new BookingId(uuid)).isNotEqualTo(new PaymentId(uuid));
    }
}
