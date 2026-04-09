package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.logic.facts.LogicFactsResolver;
import com.myudog.myulib.api.game.timer.TimerModels.TimerSnapshot;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class LogicContracts {
    private LogicContracts() {
    }

    public interface LogicSignal {
    }

    public record LogicContext<S extends GameState>(GameInstance<S> instance, LogicSignal signal, LogicFactsResolver facts, S previousState, S currentState, TimerSnapshot timerSnapshot) {
        public Identifier gameId() {
            return instance == null ? null : instance.getDefinition().getId();
        }

        public int instanceId() {
            return instance == null ? -1 : instance.getInstanceId();
        }
    }

    @FunctionalInterface
    public interface LogicCondition<S extends GameState> {
        boolean test(LogicContext<S> context);
    }

    @FunctionalInterface
    public interface LogicAction<S extends GameState> {
        void execute(LogicContext<S> context);
    }

    public record LogicRule<S extends GameState>(String id, Class<? extends LogicSignal> signalType, List<LogicCondition<S>> conditions, List<LogicAction<S>> actions, int priority) {
        public LogicRule {
            id = Objects.requireNonNullElse(id, "");
            signalType = Objects.requireNonNull(signalType, "signalType");
            conditions = conditions == null ? List.of() : List.copyOf(conditions);
            actions = actions == null ? List.of() : List.copyOf(actions);
        }

        public boolean matches(LogicSignal signal) {
            return signal != null && signalType.isInstance(signal);
        }
    }

    public record LogicRuleSet<S extends GameState>(List<LogicRule<S>> rules) {
        public LogicRuleSet {
            rules = rules == null ? List.of() : List.copyOf(rules);
        }

        public void install(LogicEngine<S> engine) {
            if (engine != null) {
                for (LogicRule<S> rule : rules) {
                    engine.register(rule);
                }
            }
        }
    }

    public static final class LogicEventBus<S extends GameState> {
        private final List<Consumer<LogicSignal>> subscribers = new CopyOnWriteArrayList<>();

        public void subscribe(Consumer<LogicSignal> subscriber) {
            subscribers.add(Objects.requireNonNull(subscriber, "subscriber"));
        }

        public void unsubscribe(Consumer<LogicSignal> subscriber) {
            subscribers.remove(subscriber);
        }

        public void dispatch(LogicSignal signal) {
            for (Consumer<LogicSignal> subscriber : subscribers) {
                subscriber.accept(signal);
            }
        }

        public List<Consumer<LogicSignal>> subscribers() {
            return Collections.unmodifiableList(new ArrayList<>(subscribers));
        }
    }
}
