package com.myudog.myulib.api.game.feature;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.game.logic.LogicContracts;
import com.myudog.myulib.api.game.logic.LogicEngine;

public class GameLogicFeature<S extends GameState> implements GameFeature {
    public final LogicEngine<S> engine;

    public GameLogicFeature() {
        this(new LogicEngine<>());
    }

    public GameLogicFeature(LogicEngine<S> engine) {
        this.engine = engine;
    }

    public void bind(GameInstance<S> instance) {
        engine.bind(instance);
    }

    public void publish(LogicContracts.LogicSignal signal) {
        engine.publish(signal);
    }

    public void publishGameCreated(GameInstance<S> instance) {
        engine.publishGameCreated(instance);
    }

    public void publishGameDestroyed(GameInstance<S> instance) {
        engine.publishGameDestroyed(instance);
    }

    public void publishGameCreatedRaw(GameInstance<S> instance) {
        publishGameCreated(instance);
    }

    public void publishGameDestroyedRaw(GameInstance<S> instance) {
        publishGameDestroyed(instance);
    }

    public void register(LogicContracts.LogicRule<S> rule) {
        engine.register(rule);
    }

    public void clearRules() {
        engine.clearRules();
    }

    public boolean isBound() {
        return engine.isBound();
    }
}
