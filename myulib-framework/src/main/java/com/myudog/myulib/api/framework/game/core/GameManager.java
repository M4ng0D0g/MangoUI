package com.myudog.myulib.api.framework.game.core;

import com.myudog.myulib.api.core.state.IState;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.internal.game.GameRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * GameManager
 *
 * 系統：遊戲核心系統 (Framework - Game)
 * 角色：全域遊戲生命週期管理中心，負責遊戲定義註冊、實例建立、玩家分配與系統清理。
 * 模式：Manager / Dispatcher
 */
public final class GameManager {

    public static final GameManager INSTANCE = new GameManager();

    private final GameRegistry REGISTRY = new GameRegistry();

    private GameManager() {
    }

    /**
     * 安裝遊戲管理系統。
     */
    public void install() {
        ServerPlayerEvents.LEAVE.register((player) -> {
            leaveAllInstances(player.getUUID());
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            onShutDown();
        });
    }

    /**
     * 伺服器關閉時的清理工作。
     */
    public void onShutDown() {
        for (GameInstance<?, ?, ?> instance : REGISTRY.instances()) {
            instance.shutdown();
        }
        REGISTRY.clearAll();
        DebugLogManager.INSTANCE.log(DebugFeature.GAME, "GameManager shutdown: all instances cleaned.");
    }

    /**
     * 註冊遊戲定義。
     */
    public void register(@NotNull GameDefinition<?, ?, ?> definition) {
        REGISTRY.registerDefinition(definition);
        DebugLogManager.INSTANCE.log(DebugFeature.GAME, "Registered definition: " + definition.id());
    }

    /**
     * 獲取所有已註冊的遊戲定義。
     */
    public Collection<GameDefinition<?, ?, ?>> getDefinitions() {
        return REGISTRY.definitions();
    }

    /**
     * 獲取遊戲定義。
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <C extends GameConfig, D extends GameData, S extends IState<IGameContext>> GameDefinition<C, D, S> getDefinition(Identifier id) {
        return (GameDefinition<C, D, S>) REGISTRY.getDefinition(id);
    }

    /**
     * 建立遊戲實例。
     */
    public <C extends GameConfig, D extends GameData, S extends IState<IGameContext>> GameInstance<C, D, S> createInstance(
            @NotNull Identifier definitionId,
            @NotNull String instanceId,
            @NotNull ServerLevel level) {
        
        if (REGISTRY.getInstance(instanceId) != null) {
            throw new IllegalArgumentException("Instance already exists: " + instanceId);
        }

        GameDefinition<C, D, S> definition = getDefinition(definitionId);
        if (definition == null) throw new IllegalArgumentException("Unknown definition: " + definitionId);

        C config = definition.createConfig();
        GameInstance<C, D, S> instance = definition.createInstance(instanceId, config, level);
        REGISTRY.addInstance(instance);
        
        DebugLogManager.INSTANCE.log(DebugFeature.GAME, "Created instance: " + instanceId + " (" + definitionId + ")");
        return instance;
    }

    /**
     * 獲取遊戲實例。
     */
    @Nullable
    public GameInstance<?, ?, ?> getInstance(@NotNull String instanceId) {
        return REGISTRY.getInstance(instanceId);
    }

    /**
     * 獲取所有運行中的遊戲實例。
     */
    public Collection<GameInstance<?, ?, ?>> getInstances() {
        return REGISTRY.instances();
    }

    /**
     * 初始化遊戲實例。
     */
    public boolean initInstance(@NotNull String instanceId) {
        var instance = REGISTRY.getInstance(instanceId);
        if (instance == null) return false;
        return instance.initialize();
    }

    /**
     * 啟動遊戲實例。
     */
    public boolean startInstance(@NotNull String instanceId) {
        var instance = REGISTRY.getInstance(instanceId);
        if (instance == null) return false;
        return instance.start();
    }

    /**
     * 關閉遊戲實例。
     */
    public boolean shutdownInstance(@NotNull String instanceId) {
        var instance = REGISTRY.getInstance(instanceId);
        if (instance == null) return false;
        return instance.shutdown();
    }

    /**
     * 刪除遊戲實例。
     */
    public boolean deleteInstance(@NotNull String instanceId) {
        var instance = REGISTRY.removeInstance(instanceId);
        if (instance == null) return false;

        REGISTRY.clearMappingsForInstance(instanceId);
        instance.destroy();
        return true;
    }

    /**
     * 玩家加入遊戲。
     */
    public boolean joinPlayer(@NotNull String instanceId, @NotNull UUID playerUuid, @Nullable UUID teamUuid) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstance(instanceId);
        if (instance == null || !instance.isInitialized()) return false;

        // 檢查玩家是否已經在其他遊戲中
        GameInstance<?, ?, ?> existing = REGISTRY.getInstanceByPlayer(playerUuid);
        if (existing != null) {
            if (existing.getInstanceId().equals(instanceId)) return true;
            return false; // 不可同時加入兩個遊戲
        }

        if (instance.joinPlayer(playerUuid, teamUuid)) {
            REGISTRY.linkPlayer(playerUuid, instanceId);
            return true;
        }
        return false;
    }

    /**
     * 玩家離開遊戲。
     */
    public void leavePlayer(@NotNull String instanceId, @NotNull UUID playerUuid) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstance(instanceId);
        if (instance != null) {
            instance.leavePlayer(playerUuid);
        }
        REGISTRY.unlinkPlayer(playerUuid);
    }

    /**
     * 玩家離開所有遊戲。
     */
    public void leaveAllInstances(@NotNull UUID playerUuid) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstanceByPlayer(playerUuid);
        if (instance != null) {
            instance.leavePlayer(playerUuid);
        }
        REGISTRY.unlinkPlayer(playerUuid);
    }

    /**
     * 註冊實體到遊戲實例的映射。
     */
    public void registerEntity(@NotNull UUID entityUuid, @NotNull String instanceId) {
        if (REGISTRY.getInstance(instanceId) == null)
            throw new IllegalArgumentException("Instance not found: " + instanceId);
        REGISTRY.linkEntity(entityUuid, instanceId);
    }

    /**
     * 取消實體映射。
     */
    public void unregisterEntity(@NotNull UUID entityUuid) {
        REGISTRY.unlinkEntity(entityUuid);
    }

    /**
     * 處理實體互動事件並派發至對應遊戲。
     */
    public boolean handleEntityInteract(net.minecraft.server.level.ServerPlayer player, Entity target, InteractionHand hand) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstanceByEntity(target.getUUID());
        if (instance == null || !instance.isStarted()) return false;
        
        // 此處原先有 EntityInteractEvent，需確保該事件存在或進行適當處理
        // 暫時保持原有邏輯結構
        return false;
    }
}
