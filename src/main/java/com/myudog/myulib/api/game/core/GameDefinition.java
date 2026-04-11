package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.game.state.GameState;
import com.myudog.myulib.api.game.state.GameStateMachine;
import com.myudog.myulib.internal.event.EventDispatcherImpl;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public abstract class GameDefinition<C extends GameConfig, D extends GameData, S extends GameState> {
    private final Identifier id;

    protected GameDefinition(Identifier id) {
        this.id = Objects.requireNonNull(id, "id 不得為空");
    }

    public final Identifier getId() {
        return id;
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

    /**
     * 核心生命週期：在此處將遊戲邏輯訂閱至房間專屬的 EventBus
     */
    public abstract void bindBehaviors(GameInstance<C, D, S> instance);

    // --- 主建構流程 (不可被覆寫) ---

    public final GameInstance<C, D, S> createInstance(int instanceId, C config) {
        C resolvedConfig = Objects.requireNonNull(config, "傳入的 config 不得為空");

        // 直接執行驗證 (若有錯會自動拋出 IllegalArgumentException)
        resolvedConfig.validate();

        D data = Objects.requireNonNull(createInitialData(resolvedConfig), "createInitialData() 不得回傳 null");
        GameStateMachine<S> stateMachine = Objects.requireNonNull(createStateMachine(resolvedConfig), "createStateMachine() 不得回傳 null");

        // 🌟 這裡呼叫 createEventBus() 時，回傳型別與變數宣告現在 100% 吻合了！
        EventDispatcherImpl eventBus = Objects.requireNonNull(createEventBus(), "createEventBus() 不得回傳 null");

        // 將 eventBus 注入 GameInstance 建構子
        GameInstance<C, D, S> instance = new GameInstance<>(instanceId, this, resolvedConfig, data, stateMachine, eventBus);

        // 實例化完成後，自動觸發事件綁定
        bindBehaviors(instance);

        return instance;
    }
}