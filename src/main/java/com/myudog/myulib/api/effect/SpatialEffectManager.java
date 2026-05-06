package com.myudog.myulib.api.effect;

import com.myudog.myulib.api.effect.component.SpatialEffectComponent;
import com.myudog.myulib.api.ecs.EcsContainer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 空間滯留效果管理器。
 * 內部維護一個獨立的 EcsContainer，專門處理玩家與 AABB 重疊時的參照計數與狀態同步。
 */
public final class SpatialEffectManager implements ISpatialEffectManager {

    public static final SpatialEffectManager INSTANCE = new SpatialEffectManager();

    private SpatialEffectManager() {}

    // 🌟 核心：內部獨立的 ECS 容器
    private final EcsContainer ecs = new EcsContainer();

    // 橋接：將 Minecraft 玩家 UUID 對應到內部 ECS 的 Entity ID
    private final Map<UUID, Integer> playerToEcsId = new ConcurrentHashMap<>();
    private final Map<Holder<MobEffect>, SpatialEffect> definitions = new ConcurrentHashMap<>();

    // 取得或建立玩家在內部 ECS 的代表實體
    private int getOrCreateEcsId(ServerPlayer player) {
        return playerToEcsId.computeIfAbsent(player.getUUID(), ignored -> {
            int newId = ecs.createEntity();
            ecs.addComponent(newId, SpatialEffectComponent.class, new SpatialEffectComponent());
            return newId;
        });
    }

    /**
     * 當玩家【踏入】某個帶有藥水效果的 AABB 時呼叫
     */
    private void addEffectSource(ServerPlayer player, MobEffect effect, int amplifier) {
        int ecsId = getOrCreateEcsId(player);
        SpatialEffectComponent comp = ecs.getComponent(ecsId, SpatialEffectComponent.class);
        Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);

        // 參照計數 +1
        int count = comp.sourceCounts.getOrDefault(effectHolder, 0);
        comp.sourceCounts.put(effectHolder, count + 1);

        // 🌟 關鍵邏輯：如果是從 0 變成 1 (第一次踏入該種效果的任何範圍)
        if (count == 0) {
            // 呼叫原生 API，給予「無限時間 (-1)」、隱藏粒子的藥水效果
            MobEffectInstance instance = new MobEffectInstance(effectHolder, MobEffectInstance.INFINITE_DURATION, amplifier, false, false, true);
            player.addEffect(instance);
        }
    }

    @Override
    public void addEffectSource(ServerPlayer player, SpatialEffect effect) {
        Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.vanillaEffect());
        definitions.put(effectHolder, effect);
        addEffectSource(player, effect.vanillaEffect(), effect.amplifier());
    }

    /**
     * 當玩家【離開】某個帶有藥水效果的 AABB 時呼叫
     */
    private void removeEffectSource(ServerPlayer player, MobEffect effect) {
        Integer ecsId = playerToEcsId.get(player.getUUID());
        if (ecsId == null) return;

        SpatialEffectComponent comp = ecs.getComponent(ecsId, SpatialEffectComponent.class);
        Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        int count = comp.sourceCounts.getOrDefault(effectHolder, 0);

        if (count > 0) {
            // 參照計數 -1
            comp.sourceCounts.put(effectHolder, count - 1);

            // 🌟 關鍵邏輯：如果計數歸零 (已經離開所有該效果的範圍)
            if (count - 1 == 0) {
                // 呼叫原生 API，徹底拔除藥水效果
                player.removeEffect(effectHolder);
                comp.sourceCounts.remove(effectHolder); // 保持 Map 乾淨
            }
        }
    }

    @Override
    public void removeEffectSource(ServerPlayer player, SpatialEffect effect) {
        removeEffectSource(player, effect.vanillaEffect());
    }

    /**
     * 玩家離開房間或遊戲結束時，清理記憶體與原生狀態
     */
    @Override
    public void clearPlayer(ServerPlayer player) {
        Integer ecsId = playerToEcsId.remove(player.getUUID());
        if (ecsId != null) {
            SpatialEffectComponent comp = ecs.getComponent(ecsId, SpatialEffectComponent.class);
            if (comp != null) {
                // 拔除所有還掛在該玩家身上的空間效果
                for (Holder<MobEffect> effect : comp.sourceCounts.keySet()) {
                    player.removeEffect(effect);
                }
            }
            ecs.destroyEntity(ecsId);
        }
    }

    @Override
    public void clearPlayer(UUID playerId) {
        Integer ecsId = playerToEcsId.remove(playerId);
        if (ecsId != null) {
            ecs.destroyEntity(ecsId);
        }
    }

    @Override
    public boolean hasAnySpatialEffect(UUID playerId) {
        Integer ecsId = playerToEcsId.get(playerId);
        if (ecsId == null) {
            return false;
        }
        SpatialEffectComponent comp = ecs.getComponent(ecsId, SpatialEffectComponent.class);
        return comp != null && !comp.sourceCounts.isEmpty();
    }

    @Override
    public boolean isSpatialEffect(UUID playerId, Holder<MobEffect> effectHolder) {
        Integer ecsId = playerToEcsId.get(playerId);
        if (ecsId == null) {
            return false;
        }
        SpatialEffectComponent comp = ecs.getComponent(ecsId, SpatialEffectComponent.class);
        return comp != null && comp.sourceCounts.containsKey(effectHolder);
    }

    @Override
    public List<SpatialEffect> getActiveEffects(UUID playerId) {
        Integer ecsId = playerToEcsId.get(playerId);
        if (ecsId == null) {
            return List.of();
        }

        SpatialEffectComponent comp = ecs.getComponent(ecsId, SpatialEffectComponent.class);
        if (comp == null || comp.sourceCounts.isEmpty()) {
            return List.of();
        }

        List<SpatialEffect> active = new ArrayList<>();
        for (Holder<MobEffect> holder : comp.sourceCounts.keySet()) {
            SpatialEffect def = definitions.get(holder);
            if (def != null) {
                active.add(def);
            }
        }
        return active;
    }
}