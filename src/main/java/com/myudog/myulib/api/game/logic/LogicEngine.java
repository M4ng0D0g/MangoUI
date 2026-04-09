package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.logic.facts.LogicFactsResolver;
import com.myudog.myulib.api.game.timer.TimerModels;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LogicEngine<S extends GameState> {
    private final List<LogicContracts.LogicRule<S>> rules = new ArrayList<>();
    private LogicFactsResolver factsResolver = LogicFactsResolver.DEFAULT;
    private GameInstance<S> instance;

    public void bind(GameInstance<S> instance) {
        this.instance = instance;
    }

    public void setFactsResolver(LogicFactsResolver resolver) {
        this.factsResolver = resolver == null ? LogicFactsResolver.DEFAULT : resolver;
    }

    public void register(LogicContracts.LogicRule<S> rule) {
        rules.add(Objects.requireNonNull(rule, "rule"));
        rules.sort(Comparator.comparingInt(LogicContracts.LogicRule<S>::priority).reversed());
    }

    public void registerAll(Iterable<LogicContracts.LogicRule<S>> rules) {
        if (rules != null) {
            for (LogicContracts.LogicRule<S> rule : rules) {
                register(rule);
            }
        }
    }

    public void clearRules() {
        rules.clear();
    }

    public boolean isBound() {
        return instance != null;
    }

    @SuppressWarnings("unchecked")
    public void publish(LogicContracts.LogicSignal signal) {
        if (instance == null || signal == null) {
            return;
        }
        S previousState = instance.getCurrentState();
        S currentState = instance.getCurrentState();
        TimerModels.TimerSnapshot timerSnapshot = null;
        if (signal instanceof LogicSignals.GameStateChangedSignal<?> stateChanged) {
            previousState = (S) stateChanged.previousState();
            currentState = (S) stateChanged.currentState();
        } else if (signal instanceof LogicSignals.TimerStartedSignal started) {
            timerSnapshot = started.snapshot();
        } else if (signal instanceof LogicSignals.TimerPausedSignal paused) {
            timerSnapshot = paused.snapshot();
        } else if (signal instanceof LogicSignals.TimerResumedSignal resumed) {
            timerSnapshot = resumed.snapshot();
        } else if (signal instanceof LogicSignals.TimerResetSignal reset) {
            timerSnapshot = reset.snapshot();
        } else if (signal instanceof LogicSignals.TimerStoppedSignal stopped) {
            timerSnapshot = stopped.snapshot();
        } else if (signal instanceof LogicSignals.TimerTickSignal tick) {
            timerSnapshot = tick.snapshot();
        } else if (signal instanceof LogicSignals.TimerCheckpointSignal checkpoint) {
            timerSnapshot = checkpoint.snapshot();
        } else if (signal instanceof LogicSignals.TimerCompletedSignal completed) {
            timerSnapshot = completed.snapshot();
        }

        LogicContracts.LogicContext<S> context = new LogicContracts.LogicContext<>(instance, signal, factsResolver, previousState, currentState, timerSnapshot);
        for (LogicContracts.LogicRule<S> rule : rules) {
            if (!rule.matches(signal)) {
                continue;
            }
            boolean passed = true;
            for (LogicContracts.LogicCondition<S> condition : rule.conditions()) {
                if (!condition.test(context)) {
                    passed = false;
                    break;
                }
            }
            if (!passed) {
                continue;
            }
            for (LogicContracts.LogicAction<S> action : rule.actions()) {
                action.execute(context);
            }
        }
    }

    public void publishStateChanged(GameInstance<S> instance, S previousState, S currentState) {
        publish(new LogicSignals.GameStateChangedSignal<>(instance, previousState, currentState));
    }

    public void publishGameCreated(GameInstance<S> instance) {
        publish(new LogicSignals.GameInstanceCreatedSignal<>(instance));
    }

    public void publishGameDestroyed(GameInstance<S> instance) {
        publish(new LogicSignals.GameInstanceDestroyedSignal<>(instance));
    }

    public void publishTimerStarted(TimerModels.TimerSnapshot snapshot) { publish(new LogicSignals.TimerStartedSignal(snapshot)); }
    public void publishTimerPaused(TimerModels.TimerSnapshot snapshot) { publish(new LogicSignals.TimerPausedSignal(snapshot)); }
    public void publishTimerResumed(TimerModels.TimerSnapshot snapshot) { publish(new LogicSignals.TimerResumedSignal(snapshot)); }
    public void publishTimerReset(TimerModels.TimerSnapshot snapshot) { publish(new LogicSignals.TimerResetSignal(snapshot)); }
    public void publishTimerStopped(TimerModels.TimerSnapshot snapshot) { publish(new LogicSignals.TimerStoppedSignal(snapshot)); }
    public void publishTimerTick(TimerModels.TimerSnapshot snapshot) { publish(new LogicSignals.TimerTickSignal(snapshot)); }
    public void publishTimerCheckpoint(TimerModels.TimerSnapshot snapshot, int bindingId, TimerModels.TimerTickBasis basis, long tick) { publish(new LogicSignals.TimerCheckpointSignal(snapshot, bindingId, basis, tick)); }
    public void publishTimerCompleted(TimerModels.TimerSnapshot snapshot) { publish(new LogicSignals.TimerCompletedSignal(snapshot)); }
}
