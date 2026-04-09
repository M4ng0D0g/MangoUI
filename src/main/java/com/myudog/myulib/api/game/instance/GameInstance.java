package com.myudog.myulib.api.game.instance;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.bootstrap.GameBootstrapConfig;
import com.myudog.myulib.api.game.bootstrap.GameObjectConfig;
import com.myudog.myulib.api.game.feature.GameComponentFeature;
import com.myudog.myulib.api.game.feature.GameFeature;
import com.myudog.myulib.api.game.feature.GameFeatureStore;
import com.myudog.myulib.api.game.feature.GameLogicFeature;
import com.myudog.myulib.api.game.feature.GameObjectBindingFeature;
import com.myudog.myulib.api.game.feature.GameRegionFeature;
import com.myudog.myulib.api.game.feature.GameScoreboardFeature;
import com.myudog.myulib.api.game.feature.GameTeamFeature;
import com.myudog.myulib.api.game.feature.GameTimerFeature;
import com.myudog.myulib.api.game.logic.LogicSignals;
import com.myudog.myulib.api.game.state.GameStateContext;
import com.myudog.myulib.api.game.state.GameDefinition;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class GameInstance<S extends GameState> {
    private final int instanceId;
    private final GameDefinition<S> definition;
    private final GameBootstrapConfig bootstrapConfig;
    private final Map<Identifier, GameObjectConfig> specialObjects;
    private final GameFeatureStore features = new GameFeatureStore();
    private boolean enabled = true;
    private S currentState;
    private long tickCount;

    public GameInstance(int instanceId, GameDefinition<S> definition, GameBootstrapConfig bootstrapConfig) {
        this.instanceId = instanceId;
        this.definition = Objects.requireNonNull(definition, "definition");
        this.bootstrapConfig = Objects.requireNonNull(bootstrapConfig, "bootstrapConfig");
        this.specialObjects = new LinkedHashMap<>(bootstrapConfig.specialObjects());
        this.currentState = definition.getInitialState();
    }

    public int getInstanceId() { return instanceId; }
    public GameDefinition<S> getDefinition() { return definition; }
    public GameBootstrapConfig getBootstrapConfig() { return bootstrapConfig; }
    public Map<Identifier, GameObjectConfig> getSpecialObjects() { return Map.copyOf(specialObjects); }
    public GameFeatureStore getFeatures() { return features; }
    public boolean isEnabled() { return enabled; }
    public S getCurrentState() { return currentState; }
    public long getTickCount() { return tickCount; }

    public <T extends GameFeature> T feature(Class<T> type) { return features.get(type); }
    public <T extends GameFeature> T requireFeature(Class<T> type) { return features.require(type); }
    public <T extends GameFeature> T putFeature(T feature) { return features.put(feature); }
    public <T extends GameFeature> T removeFeature(Class<T> type) { return features.remove(type); }
    public <T extends GameFeature> T getFeatureOrCreate(Class<T> type) { T existing = feature(type); if (existing != null) return existing; try { T created = type.getDeclaredConstructor().newInstance(); return putFeature(created); } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unable to create feature: " + type.getName(), e); } }

    @SuppressWarnings("unchecked")
    public GameLogicFeature<S> logicOrNull() { return feature(GameLogicFeature.class); }
    @SuppressWarnings("unchecked")
    public GameLogicFeature<S> logic() { return getFeatureOrCreate(GameLogicFeature.class); }
    public GameTimerFeature logicTimerFeatureOrNull() { return feature(GameTimerFeature.class); }

    public GameObjectConfig registerSpecialObject(GameObjectConfig config) {
        specialObjects.put(config.id(), config);
        getFeatureOrCreate(GameObjectBindingFeature.class).bind(config, null);
        return config;
    }

    public boolean canTransition(S to) { return enabled && definition.isTransitionAllowed(currentState, to); }

    public boolean transition(S to) {
        if (!canTransition(to)) {
            return false;
        }
        S previous = currentState;
        GameStateContext<S> context = new GameStateContext<>(definition.getId(), instanceId, previous, to);
        if (previous != null) {
            previous.onExit(this, context);
        }
        currentState = to;
        if (currentState != null) {
            currentState.onEnter(this, context);
        }
        GameLogicFeature<S> logic = logicOrNull();
        if (logic != null) {
            logic.publish(new LogicSignals.GameStateChangedSignal<>(this, previous, to));
        }
        return true;
    }

    public boolean transitionUnsafe(S to) { return transition(to); }

    public void resetState() { currentState = definition.getInitialState(); }

    public void tick() {
        if (!enabled) {
            return;
        }
        tickCount++;
        GameObjectBindingFeature objectBindings = feature(GameObjectBindingFeature.class);
        if (objectBindings != null) {
            objectBindings.tick(this);
        }
        if (currentState != null) {
            currentState.onTick(this);
        }
    }

    public void destroy() {
        if (!enabled) {
            return;
        }
        enabled = false;
        GameLogicFeature<S> logic = logicOrNull();
        if (logic != null) {
            logic.publishGameDestroyed(this);
        }
        GameObjectBindingFeature objectBindings = feature(GameObjectBindingFeature.class);
        if (objectBindings != null) {
            objectBindings.clear(this);
        }
        features.clear();
    }

    public boolean hasSpecialObject(Identifier id) { return specialObjects.containsKey(id); }
    public GameObjectConfig requireSpecialObject(Identifier id) { GameObjectConfig config = specialObjects.get(id); if (config == null) throw new IllegalStateException("Missing special object: " + id); return config; }
}
