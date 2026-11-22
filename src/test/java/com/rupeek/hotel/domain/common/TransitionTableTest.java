package com.rupeek.hotel.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

class TransitionTableTest {

    private enum Light { RED, GREEN, YELLOW, BROKEN }

    private final TransitionTable<Light> table = TransitionTable.of(Light.class)
            .allow(Light.RED, Light.GREEN)
            .allow(Light.GREEN, Light.YELLOW, Light.BROKEN)
            .allow(Light.YELLOW, Light.RED)
            .build();

    @Test
    void allowsOnlyTheConfiguredMoves() {
        assertThat(table.allows(Light.RED, Light.GREEN)).isTrue();
        assertThat(table.allows(Light.GREEN, Light.YELLOW)).isTrue();
        assertThat(table.allows(Light.GREEN, Light.BROKEN)).isTrue();
        assertThat(table.allows(Light.RED, Light.YELLOW)).isFalse();
        assertThat(table.allows(Light.RED, Light.RED)).isFalse();
    }

    @Test
    void aStateThatWasNeverConfiguredIsTerminal() {
        for (Light next : Light.values()) {
            assertThat(table.allows(Light.BROKEN, next)).isFalse();
        }
    }

    @Test
    void aSecondAllowCallAddsMoves() {
        TransitionTable<Light> extended = TransitionTable.of(Light.class)
                .allow(Light.RED, Light.GREEN)
                .allow(Light.RED, Light.YELLOW)
                .build();

        assertThat(extended.allows(Light.RED, Light.GREEN)).isTrue();
        assertThat(extended.allows(Light.RED, Light.YELLOW)).isTrue();
    }

    @Test
    void requireTransitionReturnsTheTargetState() {
        assertThat(table.requireTransition("Light", 7, Light.RED, Light.GREEN)).isEqualTo(Light.GREEN);
    }

    @Test
    void requireTransitionThrowsWithEntityIdAndStates() {
        assertThatExceptionOfType(InvalidStateTransitionException.class)
                .isThrownBy(() -> table.requireTransition("Light", 7, Light.RED, Light.YELLOW))
                .satisfies(e -> {
                    assertThat(e.entity()).isEqualTo("Light");
                    assertThat(e.id()).isEqualTo(7);
                    assertThat(e.from()).isEqualTo(Light.RED);
                    assertThat(e.to()).isEqualTo(Light.YELLOW);
                });
    }
}
