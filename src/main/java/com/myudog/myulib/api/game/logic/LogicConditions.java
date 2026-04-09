package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

public final class LogicConditions {
    private LogicConditions() {
    }

    public static <S extends GameState> LogicContracts.LogicCondition<S> always() { return LogicConditionsInternal.always(); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> stateIs(S state) { return LogicConditionsInternal.stateIs(state); }
    @SafeVarargs public static <S extends GameState> LogicContracts.LogicCondition<S> stateIn(S... states) { return LogicConditionsInternal.stateIn(states); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> hasFeature(Class<? extends com.myudog.myulib.api.game.feature.GameFeature> type) { return LogicConditionsInternal.hasFeature(type); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> specialObjectExists(net.minecraft.resources.Identifier id) { return LogicConditionsInternal.specialObjectExists(id); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> timerStatusIs(com.myudog.myulib.api.game.timer.TimerModels.TimerStatus status) { return LogicConditionsInternal.timerStatusIs(status); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> timerModeIs(com.myudog.myulib.api.game.timer.TimerModels.TimerMode mode) { return LogicConditionsInternal.timerModeIs(mode); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> timerRemainingEquals(long ticks) { return LogicConditionsInternal.timerRemainingEquals(ticks); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> timerRemainingAtMost(long ticks) { return LogicConditionsInternal.timerRemainingAtMost(ticks); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> timerElapsedAtLeast(long ticks) { return LogicConditionsInternal.timerElapsedAtLeast(ticks); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> playerCountAtLeast(int min) { return LogicConditionsInternal.playerCountAtLeast(min); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> playerCountExactly(int count) { return LogicConditionsInternal.playerCountExactly(count); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> gameTimeAtLeast(int ticks) { return LogicConditionsInternal.gameTimeAtLeast(ticks); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> gameTimeEquals(int ticks) { return LogicConditionsInternal.gameTimeEquals(ticks); }
    public static <S extends GameState> LogicContracts.LogicCondition<S> custom(java.util.function.Predicate<LogicContracts.LogicContext<S>> predicate) { return LogicConditionsInternal.custom(predicate); }
}

