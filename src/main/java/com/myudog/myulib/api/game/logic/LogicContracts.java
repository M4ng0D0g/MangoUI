package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

public final class LogicContracts {
    private LogicContracts() {
    }

    @FunctionalInterface
    public interface LogicCondition<S extends GameState> {
        boolean test(LogicContext<S> context);
    }

    @FunctionalInterface
    public interface LogicAction<S extends GameState> {
        void execute(LogicContext<S> context);
    }

    public interface LogicSignal {
    }

    public record LogicContext<S extends GameState>(Object instance, LogicSignal signal) {
    }

    public record LogicRule<S extends GameState>(String id) {
    }

    public record LogicRuleSet<S extends GameState>(java.util.List<LogicRule<S>> rules) {
    }

    public static final class LogicEventBus<S extends GameState> {
    }
}

