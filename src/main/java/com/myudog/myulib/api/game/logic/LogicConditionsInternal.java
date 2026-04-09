package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.feature.GameFeature;
import com.myudog.myulib.api.game.feature.GameTimerFeature;
import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.timer.TimerManager;
import com.myudog.myulib.api.game.timer.TimerModels;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

final class LogicConditionsInternal {
    private LogicConditionsInternal() {
    }

    static <S extends GameState> LogicContracts.LogicCondition<S> always() { return context -> true; }
    static <S extends GameState> LogicContracts.LogicCondition<S> stateIs(S state) { return context -> context.instance().getCurrentState() == state; }
    @SafeVarargs static <S extends GameState> LogicContracts.LogicCondition<S> stateIn(S... states) { Set<S> allowed = Set.copyOf(Arrays.asList(states)); return context -> allowed.contains(context.instance().getCurrentState()); }
    static <S extends GameState> LogicContracts.LogicCondition<S> hasFeature(Class<? extends GameFeature> type) { return context -> context.instance().feature(type) != null; }
    static <S extends GameState> LogicContracts.LogicCondition<S> specialObjectExists(Identifier id) { return context -> context.instance().hasSpecialObject(id); }
    static <S extends GameState> LogicContracts.LogicCondition<S> timerStatusIs(TimerModels.TimerStatus status) { return context -> firstTimerSnapshot(context.instance()).map(snapshot -> snapshot.status() == status).orElse(false); }
    static <S extends GameState> LogicContracts.LogicCondition<S> timerModeIs(TimerModels.TimerMode mode) { return context -> firstTimerSnapshot(context.instance()).map(snapshot -> snapshot.mode() == mode).orElse(false); }
    static <S extends GameState> LogicContracts.LogicCondition<S> timerRemainingEquals(long ticks) { return context -> firstTimerSnapshot(context.instance()).map(snapshot -> snapshot.remainingTicks() == ticks).orElse(false); }
    static <S extends GameState> LogicContracts.LogicCondition<S> timerRemainingAtMost(long ticks) { return context -> firstTimerSnapshot(context.instance()).map(snapshot -> snapshot.remainingTicks() <= ticks).orElse(false); }
    static <S extends GameState> LogicContracts.LogicCondition<S> timerElapsedAtLeast(long ticks) { return context -> firstTimerSnapshot(context.instance()).map(snapshot -> snapshot.elapsedTicks() >= ticks).orElse(false); }
    static <S extends GameState> LogicContracts.LogicCondition<S> playerCountAtLeast(int min) { return context -> context.facts().playerCount(context.instance()) >= min; }
    static <S extends GameState> LogicContracts.LogicCondition<S> playerCountExactly(int count) { return context -> context.facts().playerCount(context.instance()) == count; }
    static <S extends GameState> LogicContracts.LogicCondition<S> gameTimeAtLeast(int ticks) { return context -> context.facts().gameTimeTicks(context.instance()) >= ticks; }
    static <S extends GameState> LogicContracts.LogicCondition<S> gameTimeEquals(int ticks) { return context -> context.facts().gameTimeTicks(context.instance()) == ticks; }
    static <S extends GameState> LogicContracts.LogicCondition<S> custom(Predicate<LogicContracts.LogicContext<S>> predicate) { return predicate::test; }

    private static <S extends GameState> java.util.Optional<TimerModels.TimerSnapshot> firstTimerSnapshot(GameInstance<S> instance) {
        GameTimerFeature timers = instance.logicTimerFeatureOrNull();
        if (timers == null || timers.timerInstanceIds.isEmpty()) {
            return java.util.Optional.empty();
        }
        Integer timerEntityId = timers.timerInstanceIds.iterator().next();
        return java.util.Optional.ofNullable(TimerManager.getSnapshot(timerEntityId));
    }
}



