package com.rupeek.hotel.domain.common;

import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;

import java.util.*;

/**
 * The allowed moves of a state machine, for one status enum. Every status enum keeps one table, so
 * the rules live in one place and the entities share one check.
 *
 * <p>Build it with {@code TransitionTable.of(Status.class).allow(A, B, C).allow(B, C).build()}.
 * A state with no {@code allow} call has no moves: it is terminal.
 *
 * <p>When an enum holds its table in a static field, declare the field after the constants. A plain
 * static field initializer is enough.
 */
public final class TransitionTable<S extends Enum<S>> {

    private final Map<S, Set<S>> allowed;

    private TransitionTable(Map<S, Set<S>> allowed) {
        this.allowed = allowed;
    }

    public static <S extends Enum<S>> Builder<S> of(Class<S> type) {
        return new Builder<>(type);
    }

    public boolean allows(S from, S to) {
        return allowed.get(from).contains(to);
    }

    public boolean isTerminal(S state) {
        return allowed.get(state).isEmpty();
    }

    /**
     * Checks a move.
     *
     * @return {@code to}, so a caller can write {@code status = TABLE.requireTransition(...)}
     * @throws InvalidStateTransitionException if the move is not allowed
     */
    public S requireTransition(String entity, Object id, S from, S to) {
        if (!allows(from, to)) {
            throw new InvalidStateTransitionException(entity, id, from, to);
        }
        return to;
    }

    public static final class Builder<S extends Enum<S>> {

        private final Class<S> type;
        private final Map<S, Set<S>> allowed;

        private Builder(Class<S> type) {
            this.type = type;
            this.allowed = new EnumMap<>(type);
        }

        /** Allows moves from {@code from} to each state in {@code to}. A second call for the same state adds moves. */
        @SafeVarargs
        public final Builder<S> allow(S from, S... to) {
            Set<S> moves = allowed.computeIfAbsent(from, state -> EnumSet.noneOf(type));
            Collections.addAll(moves, to);
            return this;
        }

        /** Gives every state that was not configured an empty set of moves. */
        public TransitionTable<S> build() {
            for (S state : type.getEnumConstants()) {
                allowed.computeIfAbsent(state, key -> EnumSet.noneOf(type));
            }
            return new TransitionTable<>(allowed);
        }
    }
}
