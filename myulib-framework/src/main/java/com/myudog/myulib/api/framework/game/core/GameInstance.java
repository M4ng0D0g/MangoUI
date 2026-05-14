package com.myudog.myulib.api.framework.game.core;

import com.myudog.myulib.api.core.event.EventBus;
import com.myudog.myulib.api.core.state.IState;
import com.myudog.myulib.api.core.state.StateMachine;
import com.myudog.myulib.api.core.effect.SpatialEffectManager;
import com.myudog.myulib.api.core.object.event.StateChangeEvent;
import com.myudog.myulib.api.core.timer.TimerDefinition;
import com.myudog.myulib.api.core.timer.TimerManager;
import com.myudog.myulib.api.framework.team.TeamManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

/**
 * GameInstance
 * 
 * 系統：遊戲核心系統 (Framework - Game Core)
 * 角色：具體遊戲房間的運行實例，管理狀態機、事件匯流排與參與玩家。
 * 類型：Manager / Instance
 */
public class GameInstance<C extends GameConfig, D extends GameData, S extends IState<IGameContext>> implements IGameContext {

    private static final Logger LOGGER = Logger.getLogger(GameInstance.class.getName());

    private final UUID uuid;
    private final String instanceId;
    private final ServerLevel level;
    private final GameDefinition<C, D, S> definition;
    private final EventBus eventBus;

    private final StateMachine<S, IGameContext> stateMachine;
    private C config;
    private D data;

    private UUID hostUuid;
    private boolean initialized = false;
    private boolean started = false;
    private long tickCount = 0;

    public GameInstance(
            @NotNull String instanceId,
            @NotNull ServerLevel level,
            @NotNull GameDefinition<C, D, S> definition,
            @NotNull C config,
            @NotNull StateMachine<S, IGameContext> stateMachine,
            @NotNull EventBus eventBus
    ) {
        this.uuid = UUID.randomUUID();
        this.instanceId = instanceId;
        this.level = level;
        this.definition = definition;
        this.config = config;
        this.stateMachine = stateMachine;
        this.eventBus = eventBus;

        this.stateMachine.reset(this);
    }

    public UUID getUuid() { return uuid; }

    public String getInstanceId() { return instanceId; }

    @Override
    public C getConfig() { return config; }

    @Override
    public D getData() {
        if (data == null) throw new IllegalStateException("GameData not initialized yet.");
        return data;
    }

    @Override
    public EventBus getEventBus() { return eventBus; }

    public ServerLevel getLevel() { return level; }

    public GameDefinition<C, D, S> getDefinition() { return definition; }

    public boolean isInitialized() { return initialized; }

    public boolean isStarted() { return started; }

    public S getCurrentState() {
        return stateMachine.getCurrent();
    }

    @Override
    public long getTickCount() { return tickCount; }

    public boolean transition(S to) {
        if (!initialized || !stateMachine.canTransition(to)) return false;

        S previous = getCurrentState();
        if (stateMachine.transitionTo(to, this)) {
            eventBus.dispatch(new StateChangeEvent<S>(this, previous, to));
            return true;
        }
        return false;
    }

    public void forceTransition(S to) {
        if (!initialized || to == null) return;

        S previous = getCurrentState();
        stateMachine.forceTransition(to, this);
        eventBus.dispatch(new StateChangeEvent<S>(this, previous, to));
    }

    public void resetState() {
        if (!initialized) return;

        S previous = getCurrentState();
        stateMachine.reset(this);
        S current = getCurrentState();

        eventBus.dispatch(new StateChangeEvent<S>(this, previous, current));
    }

    public Optional<UUID> getHostUuid() { return Optional.ofNullable(hostUuid); }

    public void setHostUuid(@Nullable UUID hostUuid) { this.hostUuid = hostUuid; }

    public boolean isParticipating(UUID uuid) {
        return getPlayerTeam(uuid).isPresent();
    }

    public boolean joinPlayer(UUID playerId) {
        return joinPlayer(playerId, null);
    }

    public boolean joinPlayer(UUID playerId, @Nullable UUID requestedTeam) {
        if (playerId == null || data == null) return false;

        if (started && !config.allowSpectator() && !data.TEAM_FEATURE.containsParticipant(playerId)) {
            throw new IllegalStateException("Game already started and does not allow new spectators.");
        }

        UUID targetTeam = resolveJoinTeam(requestedTeam);
        boolean teamJoined = data.TEAM_FEATURE.moveParticipantToTeam(targetTeam, playerId);
        if (!teamJoined) return false;

        data.getEcsContainer().createEntity(playerId); // Uses UUID for stable identification
        SpatialEffectManager.INSTANCE.clearPlayer(playerId);

        return true;
    }

    public void leavePlayer(UUID playerId) {
        if (playerId == null || data == null) return;

        data.TEAM_FEATURE.removeParticipantFromTeams(playerId);
        SpatialEffectManager.INSTANCE.clearPlayer(playerId);
    }

    public Optional<UUID> getPlayerTeam(UUID playerId) {
        if (playerId == null || data == null) return Optional.empty();
        return Optional.ofNullable(data.TEAM_FEATURE.teamOf(playerId));
    }

    public void setConfig(C config) {
        if (initialized) throw new IllegalStateException("Cannot change config after initialization.");
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }


    public boolean initialize() {
        if (initialized) return false;

        try {
            if (config == null) throw new IllegalStateException("GameConfig is not set.");
            if (!config.validate()) throw new IllegalArgumentException("GameConfig validation failed.");

            this.data = Objects.requireNonNull(definition.createInitialData(config), "InitialData cannot be null");
            definition.bindBehavior(this);
            definition.onInit(this);

            initialized = true;
            LOGGER.info("GameInstance [" + instanceId + "] initialized successfully.");
            return true;
        }
        catch (Exception e) {
            clean();
            LOGGER.warning("GameInstance [" + instanceId + "] initialization failed: " + e.getMessage());
            return false;
        }
    }

    public boolean start() {
        if (!initialized || started) return false;

        try {
            definition.onStart(this);
            started = true;
            LOGGER.info("GameInstance [" + instanceId + "] started at state: " + getCurrentState());
            return true;
        } catch (Exception e) {
            started = false;
            throw new RuntimeException("Failed to start GameInstance", e);
        }
    }

    public boolean shutdown() {
        if (!initialized) return false;

        try {
            definition.onShutdown(this);
        } catch (Exception e) {
            LOGGER.warning("Error during GameInstance shutdown: " + e.getMessage());
        }

        clean();
        LOGGER.info("GameInstance [" + instanceId + "] shutdown completed.");
        return true;
    }

    public void clean() {
        initialized = false;
        started = false;
        tickCount = 0;

        stateMachine.reset(this);

        try {
            definition.unbindBehavior(this);
        } catch (Exception e) {
            LOGGER.warning("Error during unbinding behavior: " + e.getMessage());
        }

        eventBus.clear();
        if (data != null) {
            data.clean(this);
            data = null;
        }
    }

    public void destroy() {
        clean();
        LOGGER.info("GameInstance [" + instanceId + "] destroyed.");
    }



    private UUID resolveJoinTeam(UUID requestedTeam) {
        if (started) return config.SPECTATOR_TEAM;
        if (requestedTeam != null && TeamManager.INSTANCE.hasTeam(requestedTeam)) return requestedTeam;
        return config.SPECTATOR_TEAM;
    }

    public Identifier getDefinitionId() {
        return definition.id();
    }
}
