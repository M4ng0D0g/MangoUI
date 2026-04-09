package com.myudog.myulib.api.game;

import com.myudog.myulib.api.game.state.GameState;

import com.myudog.myulib.api.game.bootstrap.GameBootstrapConfig;
import com.myudog.myulib.api.game.bootstrap.GameObjectConfig;
import com.myudog.myulib.api.game.components.ComponentManager;
import com.myudog.myulib.api.game.feature.GameFeature;
import com.myudog.myulib.api.game.feature.GameLogicFeature;
import com.myudog.myulib.api.game.instance.GameInstance;
import com.myudog.myulib.api.identity.IdentityManager;
import com.myudog.myulib.api.permission.PermissionAdminService;
import com.myudog.myulib.api.permission.PermissionSeed;
import com.myudog.myulib.api.team.TeamManager;
import com.myudog.myulib.api.game.timer.TimerManager;
import com.myudog.myulib.api.game.state.GameDefinition;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class GameManager {
    private static final Map<Identifier, GameDefinition<?>> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<Integer, GameInstance<?>> INSTANCES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_INSTANCE_ID = new AtomicInteger(1);

    private GameManager() {
    }

    public static void install() {
    }

    public static void register(GameDefinition<?> definition) {
        DEFINITIONS.put(definition.getId(), definition);
    }

    public static GameDefinition<?> unregister(Identifier gameId) {
        TeamManager.unregisterGame(gameId);
        return DEFINITIONS.remove(gameId);
    }

    public static boolean hasDefinition(Identifier gameId) {
        return DEFINITIONS.containsKey(gameId);
    }

    @SuppressWarnings("unchecked")
    public static <S extends GameState> GameDefinition<S> definition(Identifier gameId) {
        return (GameDefinition<S>) DEFINITIONS.get(gameId);
    }

    @SuppressWarnings("unchecked")
    public static <S extends GameState> GameInstance<S> createInstance(Identifier gameId, GameBootstrapConfig config) {
        GameDefinition<S> definition = (GameDefinition<S>) Objects.requireNonNull(DEFINITIONS.get(gameId), "Unknown game definition: " + gameId);
        GameBootstrapConfig bootstrap = config == null ? new GameBootstrapConfig() : config;
        int instanceId = NEXT_INSTANCE_ID.getAndIncrement();
        GameInstance<S> instance = new GameInstance<>(instanceId, definition, bootstrap);
        for (GameFeature feature : definition.createFeatures(bootstrap)) {
            instance.putFeature(feature);
        }
        for (var group : definition.createIdentityGroups(bootstrap)) {
            IdentityManager.register(group);
        }
        for (PermissionSeed seed : definition.createPermissionSeeds(bootstrap)) {
            switch (seed.layer()) {
                case GLOBAL -> PermissionAdminService.grantGlobal(seed.grant());
                case DIMENSION -> PermissionAdminService.grantDimension(seed.scopeId(), seed.grant());
                case FIELD -> PermissionAdminService.grantField(seed.scopeId(), seed.grant());
                case USER -> PermissionAdminService.grantUser(java.util.UUID.fromString(seed.scopeId()), seed.grant());
            }
        }
        for (GameObjectConfig objectConfig : definition.createGameObjects(bootstrap)) {
            instance.registerSpecialObject(objectConfig);
        }
        instance.getFeatureOrCreate(GameLogicFeature.class).engine.setFactsResolver(definition.createLogicFactsResolver(bootstrap));
        instance.getFeatureOrCreate(GameLogicFeature.class).bind(instance);
        instance.getFeatureOrCreate(GameLogicFeature.class).engine.registerAll(definition.createLogicRules(bootstrap));
        ComponentManager.bindInstance(instance, definition.createComponentBindings(bootstrap));
        bootstrap.specialObjects().values().forEach(instance::registerSpecialObject);
        INSTANCES.put(instanceId, instance);
        instance.logicOrNull().publishGameCreated(instance);
        return instance;
    }

    public static GameInstance<?> getInstance(int instanceId) {
        return INSTANCES.get(instanceId);
    }

    public static List<GameInstance<?>> getInstances() {
        return List.copyOf(INSTANCES.values());
    }

    public static List<GameInstance<?>> getInstances(Identifier gameId) {
        return INSTANCES.values().stream().filter(instance -> instance.getDefinition().getId().equals(gameId)).toList();
    }

    public static boolean destroyInstance(int instanceId) {
        GameInstance<?> instance = INSTANCES.remove(instanceId);
        if (instance == null) {
            return false;
        }
        TeamManager.unregisterGame(instance.getDefinition().getId());
        instance.destroy();
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <S extends GameState> boolean transition(int instanceId, S to) {
        GameInstance<S> instance = (GameInstance<S>) INSTANCES.get(instanceId);
        return instance != null && instance.transition(to);
    }

    public static void tickAll() {
        for (GameInstance<?> instance : new ArrayList<>(INSTANCES.values())) {
            instance.tick();
        }
        TimerManager.update(null);
    }
}
