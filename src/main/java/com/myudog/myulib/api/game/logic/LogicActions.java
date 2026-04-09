package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

public final class LogicActions {
    private LogicActions() {
    }

    public static <S extends GameState> LogicContracts.LogicAction<S> run(java.util.function.Consumer<LogicContracts.LogicContext<S>> block) { return LogicActionsInternal.run(block); }
    public static <S extends GameState> LogicContracts.LogicAction<S> transitionTo(S state) { return LogicActionsInternal.transitionTo(state); }
    public static <S extends GameState> LogicContracts.LogicAction<S> resetState() { return LogicActionsInternal.resetState(); }
    public static <S extends GameState> LogicContracts.LogicAction<S> publish(LogicContracts.LogicSignal signal) { return LogicActionsInternal.publish(signal); }
    public static <S extends GameState> LogicContracts.LogicAction<S> startCurrentTimer() { return LogicActionsInternal.startCurrentTimer(); }
    public static <S extends GameState> LogicContracts.LogicAction<S> pauseCurrentTimer() { return LogicActionsInternal.pauseCurrentTimer(); }
    public static <S extends GameState> LogicContracts.LogicAction<S> resumeCurrentTimer() { return LogicActionsInternal.resumeCurrentTimer(); }
    public static <S extends GameState> LogicContracts.LogicAction<S> stopCurrentTimer() { return LogicActionsInternal.stopCurrentTimer(); }
    public static <S extends GameState> LogicContracts.LogicAction<S> resetCurrentTimer(boolean clearPayload) { return LogicActionsInternal.resetCurrentTimer(clearPayload); }
    public static <S extends GameState> LogicContracts.LogicAction<S> setScoreboardLine(int index, String value) { return LogicActionsInternal.setScoreboardLine(index, value); }
    public static <S extends GameState> LogicContracts.LogicAction<S> setScoreboardValue(String key, int value) { return LogicActionsInternal.setScoreboardValue(key, value); }
    public static <S extends GameState> LogicContracts.LogicAction<S> attachGameObject(net.minecraft.resources.Identifier id, Object runtimeObject) { return LogicActionsInternal.attachGameObject(id, runtimeObject); }
}

