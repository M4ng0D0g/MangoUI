package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.state.GameStateMachine;
import com.myudog.myulib.internal.event.EventDispatcherImpl;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.UUID;

public abstract class GameDefinition<C extends GameConfig, D extends GameData, S extends GameState> {
    private final Identifier id;

    protected GameDefinition(Identifier id) {
        this.id = Objects.requireNonNull(id, "id 不得為空");
    }

    public final Identifier getId() {
        return id;
    }

    public String gameToken() {
        return id.getPath();
    }

    public String modToken() {
        return id.getNamespace();
    }

    // --- 抽象工廠方法 (供開發者實作) ---

    /**
     * 定義如何建立這場遊戲的初始動態資料
     */
    public abstract D createInitialData(C config);

    /**
     * 定義這場遊戲的狀態機與狀態流轉規則
     */
    public abstract GameStateMachine<S> createStateMachine(C config);

    /**
     * 建立房間專屬的事件匯流排。
     * 🌟 修正：回傳型別改為 EventDispatcherImpl
     */
    protected abstract EventDispatcherImpl createEventBus();

    public final void init(GameInstance<C, D, S> instance) throws Exception {
        Objects.requireNonNull(instance, "instance");
        bindBehavior(instance);
    }

    public final void clean(GameInstance<C, D, S> instance) throws Exception {
        Objects.requireNonNull(instance, "instance");
        unbindBehavior(instance);
    }

    protected abstract void bindBehavior(GameInstance<C, D, S> instance) throws Exception;

    protected abstract void unbindBehavior(GameInstance<C, D, S> instance) throws Exception;

    protected Identifier resolveTeamForJoin(GameInstance<C, D, S> instance, UUID playerUuid, Identifier requestedTeamId) {
        return requestedTeamId;
    }

    protected void onStart(GameInstance<C, D, S> instance) throws Exception {
    }

    protected void onShutDown(GameInstance<C, D, S> instance) throws Exception {
    }

    // --- 主建構流程 (不可被覆寫) ---

    public final GameInstance<C, D, S> createInstance(int instanceId, C config, ServerLevel level) {
        try {
            C resolvedConfig = Objects.requireNonNull(config, "傳入的 config 不得為空");

            GameStateMachine<S> stateMachine = Objects.requireNonNull(createStateMachine(resolvedConfig), "createStateMachine() 不得回傳 null");

            // 🌟 這裡呼叫 createEventBus() 時，回傳型別與變數宣告現在 100% 吻合了！
            EventDispatcherImpl eventBus = Objects.requireNonNull(createEventBus(), "createEventBus() 不得回傳 null");

            // 將 eventBus 注入 GameInstance 建構子
            GameInstance<C, D, S> instance = new GameInstance<>(instanceId, level, this, resolvedConfig, stateMachine, eventBus);
            return instance;
        }
        catch (Exception e) {
            throw new RuntimeException("創建遊戲實例失敗: " + e.getMessage(), e);
        }
    }

    public final void initInstance(GameInstance<C, D, S> instance) {
        Objects.requireNonNull(instance, "instance 不得為空");
        instance.init();
    }
}