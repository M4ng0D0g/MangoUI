package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.event.EventBus;
import com.myudog.myulib.api.state.IState;
import com.myudog.myulib.api.state.StateMachine;
import com.myudog.myulib.api.effect.SpatialEffectManager;
import com.myudog.myulib.api.object.event.StateChangeEvent;
import com.myudog.myulib.api.team.TeamManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

/**
 * GameInstance 負責管理單局遊戲的運行實例。
 * 它整合了狀態機 (FSM)、事件系統 (EventBus) 與組件系統 (ECS)，
 * 並驅動遊戲從初始化到銷毀的完整生命週期。
 */
public class GameInstance<C extends GameConfig, D extends GameData, S extends IState<IGameContext>> implements IGameContext {

    private static final Logger LOGGER = Logger.getLogger(GameInstance.class.getName());

    // --- 基礎標識與環境 ---
//    private final String name;
    private final UUID uuid;

    private final ServerLevel level;
    private final GameDefinition<C, D, S> definition;
    private final EventBus eventBus; // PLAN: 交給Manager

    // --- 核心組件 ---
    private final StateMachine<S, IGameContext> stateMachine;
    private C config;
    private D data;

    // --- 運行狀態與標記 ---
    private UUID hostUuid;
    private boolean initialized = false;
    private boolean started = false;
    private long tickCount = 0;

    public GameInstance(
            @NotNull ServerLevel level,
            @NotNull GameDefinition<C, D, S> definition,
            @NotNull C config,
            @NotNull StateMachine<S, IGameContext> stateMachine,
            @NotNull EventBus eventBus
    ) {
        this.uuid = UUID.randomUUID();
        this.level = level;
        this.definition = definition;
        this.config = config;
        this.stateMachine = stateMachine;
        this.eventBus = eventBus;

        // 初始化狀態機至初始狀態
        this.stateMachine.reset(this);
    }

    // ==========================================================================================
    // 基礎存取 API
    // ==========================================================================================

    public UUID getUuid() { return uuid; }

    @Override
    public C getConfig() { return config; }

    @Override
    public D getData() {
        if (data == null) throw new IllegalStateException("GameData 尚未建立，請在初始化成功後再取得");
        return data;
    }

    @Override
    public EventBus getEventBus() { return eventBus; }

    public ServerLevel getLevel() { return level; }

    public GameDefinition<C, D, S> getDefinition() { return definition; }

    public boolean isEnabled() { return initialized; }

    public long getTickCount() { return tickCount; }

    public boolean isInitialized() { return initialized; }

    public boolean isStarted() { return started; }

    // ==========================================================================================
    // 狀態管理 (FSM Delegation)
    // ==========================================================================================

    public S getCurrentState() {
        return stateMachine.getCurrent();
    }

    /**
     * 嘗試切換至指定狀態，會經過 canTransition 檢查。
     */
    public boolean transition(S to) {
        if (!initialized || !stateMachine.canTransition(to)) return false;

        S previous = getCurrentState();
        if (stateMachine.transitionTo(to, this)) {
            eventBus.dispatch(new StateChangeEvent<>(this, previous, to));
            return true;
        }
        return false;
    }

    /**
     * 強制切換至指定狀態，無視轉換規則限制。
     */
    public void forceTransition(S to) {
        if (!initialized || to == null) return;

        S previous = getCurrentState();
        stateMachine.forceTransition(to, this);
        eventBus.dispatch(new StateChangeEvent<>(this, previous, to));
    }

    /**
     * 將狀態機重置回初始狀態。
     */
    public void resetState() {
        if (!initialized) return;

        S previous = getCurrentState();
        stateMachine.reset(this);
        S current = getCurrentState();

        eventBus.dispatch(new StateChangeEvent<>(this, previous, current));
    }

    // ==========================================================================================
    // 玩家與隊伍管理
    // ==========================================================================================

    public Optional<UUID> getHostUuid() { return Optional.ofNullable(hostUuid); }

    public void setHostUuid(@Nullable UUID hostUuid) { this.hostUuid = hostUuid; }

    public void clearHostUuid() { this.hostUuid = null; }

    public boolean isParticipating(UUID uuid) {
        return getPlayerTeam(uuid).isPresent();
    }

    /**
     * 讓玩家加入遊戲。
     */
    public boolean joinPlayer(UUID playerId) {
        return joinPlayer(playerId, null);
    }

    /**
     * 處理玩家加入邏輯，包含隊伍分配與人數檢查。
     */
    public boolean joinPlayer(UUID playerId, @Nullable UUID requestedTeam) {
        if (playerId == null || data == null) return false;

        // 1. 檢查觀戰限制
        if (started && !config.allowSpectator() && !data.TEAM_FEATURE.containsParticipant(playerId)) {
            throw new IllegalStateException("遊戲已開始且不允許觀戰加入");
        }

        // 2. 決定目標隊伍
        UUID targetTeam = resolveJoinTeam(requestedTeam);
        boolean teamJoined = data.TEAM_FEATURE.moveParticipantToTeam(targetTeam, playerId);
        if (!teamJoined) return false;

        // 3. 創建或取得專屬 ECS 實體
        data.ECS_FEATURE.getOrCreateParticipant(playerId);

        // 4. 清理可能遺留的空間效果，避免重複加入時同步失配
        SpatialEffectManager.INSTANCE.clearPlayer(playerId);

        return true;
    }

    /**
     * 處理玩家離開邏輯，同步清理隊伍與 ECS 狀態。
     */
    public void leavePlayer(UUID playerId) {
        if (playerId == null || data == null) return;

        // 1. 從隊伍系統中移除
        data.TEAM_FEATURE.removeParticipantFromTeams(playerId);

        // 2. 銷毀 ECS 實體及其所有綁定組件
        data.ECS_FEATURE.removeParticipant(playerId);

        // 3. 一併清理空間效果狀態
        SpatialEffectManager.INSTANCE.clearPlayer(playerId);
    }

    public Optional<UUID> getPlayerTeam(UUID playerId) {
        if (playerId == null || data == null) return Optional.empty();
        return Optional.ofNullable(data.TEAM_FEATURE.teamOf(playerId));
    }

    // ==========================================================================================
    // 遊戲生命週期控制
    // ==========================================================================================

    public void setConfig(C config) {
        if (initialized) throw new IllegalStateException("遊戲已初始化，無法修改配置");
        this.config = Objects.requireNonNull(config, "配置不得為空");
    }

    /**
     * 初始化遊戲實例：建立資料容器、掛載遊戲物件與執行自定義初始化邏輯。
     */
    public boolean initialize() {
        if (initialized) return false;

        try {
            // config
            if (config == null) throw new IllegalStateException("尚未設定 GameConfig");
            if (!config.validate()) throw new IllegalArgumentException("GameConfig 校驗失敗");

            // 建立初始資料
            this.data = Objects.requireNonNull(definition.createInitialData(config), "InitialData 不得為空");

            // 執行自定義行為定義
            definition.bindBehavior(this);

            initialized = true;
            LOGGER.info("GameInstance [" + uuid + "] initialized successfully with config: " + config);
            return true;
        }
        catch (Exception e) {
            clean();
            LOGGER.warning("GameInstance [" + uuid + "] initialization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 正式啟動遊戲流程。
     */
    public boolean start() {
        if (!initialized || started) return false;

        try {
            definition.onStart(this);
            started = true;
            LOGGER.info("GameInstance [" + uuid + "] at state: " + getCurrentState());
            return true;
        } catch (Exception e) {
            started = false;
            throw new RuntimeException("啟動遊戲實例失敗", e);
        }
    }

    /**
     * 正常結束遊戲。
     */
    public boolean shutdown() {
        if (!initialized) return false;

        try {
            definition.onShutdown(this);
        } catch (Exception e) {
            throw new RuntimeException("結束遊戲行為執行異常", e);
        }

        clean();
        LOGGER.info("GameInstance [" + uuid + "] shutdown resources cleaned");
        return true;
    }

    /**
     * 徹底銷毀實例，清理所有資源並標記為停用。
     */
    public void destroy() {
        clean();
        GameManager.INSTANCE.destroyInstance(uuid);
        LOGGER.info("GameInstance [" + uuid + "] destroyed successfully");
    }

    // ----------------------------------------------------------------------------------------------------
    
    /**
     * 每 Tick 驅動邏輯。
     */
    public void tick() {
        if (!initialized) return;
        tickCount++;
        stateMachine.tick(this);
    }

    

    public void clean() {
        initialized = false;
        started = false;
        tickCount = 0;

        // 1. 重置狀態機
        stateMachine.reset(this);

        // 2. 清理行為定義
        try {
            definition.unbindBehavior(this);
        } catch (Exception e) {
            throw new RuntimeException("清理行為定義失敗", e);
        }

        // 3. 清理事件與資料容器
        eventBus.clear();
        if (data != null) {
            data.clean(this);
            data = null;
        }
    }
    

    // ==========================================================================================
    // 內部輔助與清理邏輯
    // ==========================================================================================

    private UUID resolveJoinTeam(UUID requestedTeam) {
        if (started) return config.SPECTATOR_TEAM;
//        if (decidedTeam != null && data.isTeamRegistered(decidedTeam)) return decidedTeam;
        if (requestedTeam != null && TeamManager.INSTANCE.hasTeam(requestedTeam)) return requestedTeam;
        return config.SPECTATOR_TEAM;
    }

    public Identifier getDefinitionId() {
        return definition.id();
    }
}