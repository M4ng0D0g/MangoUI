package com.myudog.myulib.api.game.logic;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.feature.GameScoreboardFeature;
import com.myudog.myulib.api.game.feature.GameTimerFeature;
import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.timer.TimerManager;

import java.util.function.Consumer;

final class LogicActionsInternal {
    private LogicActionsInternal() {
    }

    static <S extends GameState> LogicContracts.LogicAction<S> run(Consumer<LogicContracts.LogicContext<S>> block) { return block::accept; }
    static <S extends GameState> LogicContracts.LogicAction<S> transitionTo(S state) { return context -> context.instance().transition(state); }
    static <S extends GameState> LogicContracts.LogicAction<S> resetState() { return context -> context.instance().resetState(); }
    static <S extends GameState> LogicContracts.LogicAction<S> publish(LogicContracts.LogicSignal signal) { return context -> { var logic = context.instance().logicOrNull(); if (logic != null) logic.publish(signal); }; }
    static <S extends GameState> LogicContracts.LogicAction<S> startCurrentTimer() { return context -> firstTimerId(context.instance()).ifPresent(TimerManager::start); }
    static <S extends GameState> LogicContracts.LogicAction<S> pauseCurrentTimer() { return context -> firstTimerId(context.instance()).ifPresent(TimerManager::pause); }
    static <S extends GameState> LogicContracts.LogicAction<S> resumeCurrentTimer() { return context -> firstTimerId(context.instance()).ifPresent(TimerManager::resume); }
    static <S extends GameState> LogicContracts.LogicAction<S> stopCurrentTimer() { return context -> firstTimerId(context.instance()).ifPresent(TimerManager::stop); }
    static <S extends GameState> LogicContracts.LogicAction<S> resetCurrentTimer(boolean clearPayload) { return context -> firstTimerId(context.instance()).ifPresent(id -> TimerManager.reset(id, clearPayload)); }
    static <S extends GameState> LogicContracts.LogicAction<S> setScoreboardLine(int index, String value) { return context -> context.instance().getFeatureOrCreate(com.myudog.myulib.api.game.feature.GameScoreboardFeature.class).setLine(index, value); }
    static <S extends GameState> LogicContracts.LogicAction<S> setScoreboardValue(String key, int value) { return context -> context.instance().getFeatureOrCreate(com.myudog.myulib.api.game.feature.GameScoreboardFeature.class).setValue(key, value); }
    static <S extends GameState> LogicContracts.LogicAction<S> attachGameObject(net.minecraft.resources.Identifier id, Object runtimeObject) { return context -> context.instance().getFeatureOrCreate(com.myudog.myulib.api.game.feature.GameObjectBindingFeature.class).attachRuntime(id, runtimeObject); }

    private static <S extends GameState> java.util.Optional<Integer> firstTimerId(GameInstance<S> instance) {
        GameTimerFeature timers = instance.getFeatureOrCreate(com.myudog.myulib.api.game.feature.GameTimerFeature.class);
        return timers.timerInstanceIds.stream().findFirst();
    }
}


