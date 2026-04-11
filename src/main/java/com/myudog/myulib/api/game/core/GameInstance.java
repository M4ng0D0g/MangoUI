package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.state.GameStateMachine;
import com.myudog.myulib.api.game.state.GameStateChangeEvent; // 假設的事件類別
import com.myudog.myulib.api.game.object.GameObjectConfig;
import com.myudog.myulib.internal.event.EventDispatcherImpl; // 使用具體的派發器實作
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;

public class GameInstance<C extends GameConfig, D extends GameData, S extends GameState> {

    private final int instanceId;
    private final GameDefinition<C, D, S> definition;

    // C-D-S 核心三本柱
    private final C config;
    private final D data;
    private final GameStateMachine<S> stateMachine;

    // 專屬事件派發器 (確保房間隔離)
    private final EventDispatcherImpl eventBus;

    private boolean enabled = true;
    private long tickCount = 0;

    public GameInstance(
            int instanceId,
            GameDefinition<C, D, S> definition,
            C config,
            D data,
            GameStateMachine<S> stateMachine,
            EventDispatcherImpl eventBus) {

        this.instanceId = instanceId;
        this.definition = Objects.requireNonNull(definition, "definition 不得為空");
        this.config = Objects.requireNonNull(config, "config 不得為空");
        this.data = Objects.requireNonNull(data, "data 不得為空");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine 不得為空");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus 不得為空");
    }

    // --- Getters ---
    public int getInstanceId() { return instanceId; }
    public GameDefinition<C, D, S> getDefinition() { return definition; }
    public C getConfig() { return config; }
    public D getData() { return data; }
    public GameStateMachine<S> getStateMachine() { return stateMachine; }
    public EventDispatcherImpl getEventBus() { return eventBus; }

    public boolean isEnabled() { return enabled; }
    public long getTickCount() { return tickCount; }

    public S getCurrentState() {
        return stateMachine.getCurrent();
    }

    // --- 狀態切換邏輯 ---

    public boolean canTransition(S to) {
        return enabled && stateMachine.canTransition(to);
    }

    /**
     * 標準狀態切換，會觸發事件
     */
    public boolean transition(S to) {
        if (!canTransition(to)) {
            return false;
        }

        S from = stateMachine.getCurrent();
        if (stateMachine.transitionTo(to)) {
            // 🌟 修正：發送狀態變更事件，讓 bindBehaviors 和 IGameEntity 能夠響應
            eventBus.dispatch(new GameStateChangeEvent<>(this, from, to));
            return true;
        }
        return false;
    }

    /**
     * 強制切換狀態，無視規則，同樣會觸發事件
     */
    public boolean transitionUnsafe(S to) {
        if (!enabled) return false;

        S from = stateMachine.getCurrent();
        // 🌟 修正：使用狀態機的強制切換方法
        stateMachine.forceTransition(to);
        eventBus.dispatch(new GameStateChangeEvent<>(this, from, to));
        return true;
    }

    public void resetState() {
        stateMachine.reset();
        // 重置狀態後通常也需要發送事件通知系統
        eventBus.dispatch(new GameStateChangeEvent<>(this, null, stateMachine.getCurrent()));
    }

    // --- 生命週期 ---

    public void tick() {
        if (!enabled) return;
        tickCount++;
    }

    public void destroy() {
        if (!enabled) return;
        this.enabled = false;

        // 🌟 修正：清理資料，防止實體或資料殘留
        this.data.reset();

        // 如果您的 EventDispatcherImpl 支援清理所有監聽器，建議在此呼叫
        // this.eventBus.clearListeners();
    }

    // --- 物件配置委派 (對齊新版 List 結構) ---

    /**
     * 檢查配置中是否定義了特定 ID 的遊戲物件
     */
    public boolean hasGameObject(Identifier id) {
        return config.gameObjects().stream()
                .anyMatch(obj -> obj.id().equals(id));
    }

    /**
     * 獲取特定 ID 的物件配置
     */
    public Optional<GameObjectConfig<?>> getGameObjectConfig(Identifier id) {
        return config.gameObjects().stream()
                .filter(obj -> obj.id().equals(id))
                .findFirst();
    }

    /**
     * 強制獲取特定 ID 的物件配置，若不存在則拋出例外
     */
    public GameObjectConfig<?> requireGameObjectConfig(Identifier id) {
        return getGameObjectConfig(id)
                .orElseThrow(() -> new IllegalStateException("遺失必要的遊戲物件配置: " + id));
    }
}