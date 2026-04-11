package com.myudog.myulib.api.game;

import com.myudog.myulib.api.game.core.GameConfig;
import com.myudog.myulib.api.game.core.GameData;
import com.myudog.myulib.api.game.core.GameInstance;
import com.myudog.myulib.api.game.core.GameDefinition;
import com.myudog.myulib.api.game.state.GameState;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class GameManager {
    private static final Map<Identifier, GameDefinition<?, ?, ?>> DEFINITIONS = new LinkedHashMap<>();

    // 使用 ConcurrentHashMap 確保執行緒安全
    private static final Map<Integer, GameInstance<?, ?, ?>> INSTANCES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_INSTANCE_ID = new AtomicInteger(1);

    private GameManager() {
        // 工具類別，禁止實例化
    }

    public static void install() {
        // 供 ModInitializer 呼叫的初始化入口
    }

    // --- Definition 管理 ---

    public static void register(GameDefinition<?, ?, ?> definition) {
        Objects.requireNonNull(definition, "GameDefinition 不能為空");
        DEFINITIONS.put(definition.getId(), definition);
    }

    public static GameDefinition<?, ?, ?> unregister(Identifier gameId) {
        return DEFINITIONS.remove(gameId);
    }

    public static boolean hasDefinition(Identifier gameId) {
        return DEFINITIONS.containsKey(gameId);
    }

    @SuppressWarnings("unchecked")
    public static <C extends GameConfig, D extends GameData, S extends GameState> GameDefinition<C, D, S> definition(Identifier gameId) {
        return (GameDefinition<C, D, S>) DEFINITIONS.get(gameId);
    }

    // --- Instance 管理 ---

    @SuppressWarnings("unchecked")
    public static <C extends GameConfig, D extends GameData, S extends GameState> GameInstance<C, D, S> createInstance(Identifier gameId, C config) {
        GameDefinition<C, D, S> definition = definition(gameId);
        if (definition == null) {
            throw new IllegalArgumentException("找不到該遊戲藍圖 (Unknown game definition): " + gameId);
        }

        // 🌟 修正 1：嚴格禁止 Null Config，避免轉型地雷
        Objects.requireNonNull(config, "建立遊戲必須提供 GameConfig 實例");

        // 🌟 修正 2：在實例化之前，先執行驗證！如果有錯會直接拋出 Exception 被指令端接住
        config.validate();

        int instanceId = NEXT_INSTANCE_ID.getAndIncrement();

        // 呼叫 Definition 建立實例 (此處您的 Definition 應該會將 EventBus 等依賴注入進去)
        GameInstance<C, D, S> instance = definition.createInstance(instanceId, config);
        INSTANCES.put(instanceId, instance);

        return instance;
    }

    public static GameInstance<?, ?, ?> getInstance(int instanceId) {
        return INSTANCES.get(instanceId);
    }

    public static List<GameInstance<?, ?, ?>> getInstances() {
        return List.copyOf(INSTANCES.values());
    }

    public static List<GameInstance<?, ?, ?>> getInstances(Identifier gameId) {
        return INSTANCES.values().stream()
                .filter(instance -> instance.getDefinition().getId().equals(gameId))
                .toList();
    }

    public static boolean destroyInstance(int instanceId) {
        GameInstance<?, ?, ?> instance = INSTANCES.remove(instanceId);
        if (instance != null) {
            instance.destroy(); // 觸發內部清理 (Data reset, EventBus unregister)
            return true;
        }
        return false;
    }

    // 🌟 修正 3：移除了原本危險盲目轉型的 GameManager.transition()
    // 開發者應改用：GameManager.getInstance(id).transition(state) 來確保型別安全

    // --- 生命週期 ---

    public static void tickAll() {
        // 🌟 修正 4：ConcurrentHashMap 支援安全的直接迭代，不再 new ArrayList 浪費 GC
        for (Map.Entry<Integer, GameInstance<?, ?, ?>> entry : INSTANCES.entrySet()) {
            GameInstance<?, ?, ?> instance = entry.getValue();

            // 🌟 修正 5：惰性清理機制 (Lazy Cleanup)
            // 如果房間被內部邏輯 destroy() 了，我們在這裡順手把它從 Map 裡拔掉
            if (!instance.isEnabled()) {
                INSTANCES.remove(entry.getKey());
                continue;
            }

            instance.tick();
        }
    }
}