package com.myudog.myulib.api.game.core;

import com.myudog.myulib.api.state.IState;
import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.effect.ISpatialEffectManager;
import com.myudog.myulib.api.effect.SpatialEffectManager;
import com.myudog.myulib.api.object.event.*;
import com.myudog.myulib.internal.game.GameRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class GameManager {

    public static final GameManager INSTANCE = new GameManager();
    
    private final GameRegistry REGISTRY = new GameRegistry();
    private final ISpatialEffectManager globalEffectManager = SpatialEffectManager.INSTANCE;

    private GameManager() {}

    public void install() {
        // 玩家離開事件：統一走 GameInstance 的離開門面做同步清理
        ServerPlayerEvents.LEAVE.register((player) -> {
            GameInstance<?, ?, ?> instance = REGISTRY.getInstanceByEntity(player.getUUID());
            if (instance != null) {
                unassignPlayer(player.getUUID());
            }
        });
    }

    // ==========================================================================================
    // 遊戲藍圖 (Definition) 管理 - 使用 UUID 作為 Key
    // ==========================================================================================

    public void register(@NotNull GameDefinition<?, ?, ?> definition) {
        REGISTRY.registerDefinition(definition);
        DebugLogManager.INSTANCE.log(DebugFeature.GAME, "Registered definition: " + definition.id());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <C extends GameConfig, D extends GameData, S extends IState<IGameContext>> GameDefinition<C, D, S> getDefinition(UUID id) {
        return (GameDefinition<C, D, S>) REGISTRY.getDefinition(id);
    }

    // ==========================================================================================
    // 遊戲實例 (Instance) 管理 - 使用 Identifier 作為 Key
    // ==========================================================================================

    public <C extends GameConfig, D extends GameData, S extends IState<IGameContext>> GameInstance<C, D, S> createInstance(
            @NotNull UUID definitionId,
            @NotNull C config,
            @NotNull ServerLevel level)
    {
        GameDefinition<C, D, S> definition = getDefinition(definitionId);
        if (definition == null) throw new IllegalArgumentException("Unknown definition: " + definitionId);
        
        GameInstance<C, D, S> instance = definition.createInstance(config, level);
        REGISTRY.addInstance(instance);
        return instance;
    }

    @Nullable
    public GameInstance<?, ?, ?> getInstance(@NotNull UUID uuid) {
        return REGISTRY.getInstance(uuid);
    }

    public Collection<GameInstance<?, ?, ?>> getInstances() {
        return REGISTRY.instances();
    }

    public boolean initInstance(@NotNull UUID uuid) {
        var instance = REGISTRY.getInstance(uuid);
        if (instance == null) return false;
        
        return instance.initialize();
    }
    
    public boolean startInstance(@NotNull UUID uuid) {
        var instance = REGISTRY.getInstance(uuid);
        if (instance == null) return false;
        
        return instance.start();
    }

    public boolean shutdownInstance(@NotNull UUID uuid) {
        var instance = REGISTRY.getInstance(uuid);
        if (instance == null) return false;
        
        return instance.shutdown();
    }

    public boolean destroyInstance(@NotNull UUID uuid) {
        var instance = REGISTRY.removeInstance(uuid);
        if (instance == null) return false;
        
        REGISTRY.clearMappingsForInstance(uuid);
        instance.destroy();
        return true;
    }

    // ==========================================================================================
    // 玩家與實體映射 API
    // ==========================================================================================

    public boolean joinPlayer(@NotNull UUID instanceUuid, @NotNull UUID playerUuid, @Nullable UUID teamUuid) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstance(instanceUuid);
        if (instance == null || !instance.isInitialized()) return false;

        // 檢查玩家是否已在其他遊戲中
        GameInstance<?, ?, ?> existing = REGISTRY.getInstanceByPlayer(playerUuid);
        if (existing != null && !existing.getUuid().equals(instanceUuid)) return false;

        if (instance.joinPlayer(playerUuid, teamUuid)) {
            REGISTRY.linkPlayer(playerUuid, instanceUuid);
            return true;
        }
        return false;
    }

    public void unassignPlayer(@NotNull UUID playerUuid) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstanceByPlayer(playerUuid);
        if (instance != null) {
            instance.leavePlayer(playerUuid);
        }
        REGISTRY.unlinkPlayer(playerUuid);
    }

    public void registerEntity(@NotNull UUID entityUuid, @NotNull UUID instanceUuid) {
        if (REGISTRY.getInstance(instanceUuid) == null) throw new IllegalArgumentException("Instance not found: " + instanceUuid);
        REGISTRY.linkEntity(entityUuid, instanceUuid);
    }

    public void unregisterEntity(@NotNull UUID entityUuid) {
        REGISTRY.unlinkEntity(entityUuid);
    }

    public boolean handleEntityInteract(net.minecraft.server.level.ServerPlayer player, Entity target, InteractionHand hand) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstanceByEntity(target.getUUID());
        if (instance == null || !instance.isStarted()) return false;
        EntityInteractEvent event = new EntityInteractEvent(player, target, hand);
        instance.getEventBus().dispatch(event);
        return event.isCanceled();
    }

    public boolean handleBlockBreak(net.minecraft.server.level.ServerPlayer player, BlockPos pos, ServerLevel level) {
        GameInstance<?, ?, ?> instance = getInstanceByLevel(level);
        if (instance == null || !instance.isStarted()) return false;
        BlockBreakEvent event = new BlockBreakEvent(player, pos, level);
        instance.getEventBus().dispatch(event);
        return event.isCanceled();
    }

    public boolean handleBlockInteract(net.minecraft.server.level.ServerPlayer player, BlockPos pos, ServerLevel level) {
        GameInstance<?, ?, ?> instance = getInstanceByLevel(level);
        if (instance == null || !instance.isStarted()) return false;
        BlockInteractEvent event = new BlockInteractEvent(player, pos, level);
        instance.getEventBus().dispatch(event);
        return event.isCanceled();
    }

    public ISpatialEffectManager getGlobalEffectManager() {
        return globalEffectManager;
    }

    private GameInstance<?, ?, ?> getInstanceByLevel(ServerLevel level) {
        for (GameInstance<?, ?, ?> instance : REGISTRY.instances()) {
            if (instance.getLevel() == level) return instance;
        }
        return null;
    }

    // ==========================================================================================
    // 生命週期與事件派發
    // ==========================================================================================

    public void tickAll() {
        REGISTRY.getAllInstances().removeIf(instance -> {
            if (!instance.isInitialized()) return true;
            
            instance.tick();
            return false;
        });
    }

    /**
     * 利用反向索引極速派發實體受傷/死亡事件。
     */
    public void handleEntityDamage(LivingEntity victim, DamageSource source, float amount) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstanceByEntity(victim.getUUID());
        if (instance != null && instance.isStarted()) {
            instance.getEventBus().dispatch(new EntityDamageEvent(victim, source, amount));
        }
    }

    public void handleEntityDeath(LivingEntity victim, DamageSource source) {
        GameInstance<?, ?, ?> instance = REGISTRY.getInstanceByEntity(victim.getUUID());
        if (instance != null && instance.isStarted()) {
            instance.getEventBus().dispatch(new EntityDeathEvent(victim, source));
        }
        REGISTRY.unlinkEntity(victim.getUUID());
    }
}