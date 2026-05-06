package com.myudog.myulib.api.game.features;

import com.myudog.myulib.api.ecs.EcsContainer;
import com.myudog.myulib.api.game.core.GameInstance;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EcsFeatureImpl implements EcsFeature {

    // 遵守 Java 命名規範，實例變數使用小駝峰
    private final EcsContainer container = new EcsContainer();

    // 🌟 核心升級：使用 ConcurrentHashMap 確保玩家並發加入/退出時的執行緒安全
    private final Map<UUID, Integer> participantToEntity = new ConcurrentHashMap<>();

    public EcsFeatureImpl() {}

    @Override
    public EcsContainer getContainer() {
        return container;
    }

    @Override
    public Optional<Integer> getEntity(@NotNull UUID uuid) {
        // 🌟 效能優化：避免 containsKey + get 的兩次查找，直接取得並包裝
        return Optional.ofNullable(participantToEntity.get(uuid));
    }

    @Override
    public int getOrCreateParticipant(@NotNull UUID uuid) {
        // 🌟 原子操作：如果沒有該 UUID，才會呼叫 container.createEntity()，並保證執行緒安全
        return participantToEntity.computeIfAbsent(uuid, k -> container.createEntity());
    }

    @Override
    public int removeParticipant(@NotNull UUID uuid) {
        // 直接移除並獲取舊值，避免多次查詢
        Integer entityId = participantToEntity.remove(uuid);

        if (entityId == null) {
            return -1;
        }

        // 確實銷毀底層 ECS 容器中的實體
        container.destroyEntity(entityId);
        return entityId;
    }

    @Override
    public void clean(GameInstance<?, ?, ?> instance) {
        // 🌟 實作清理邏輯：銷毀所有綁定的玩家實體
        for (Integer entityId : participantToEntity.values()) {
            container.destroyEntity(entityId);
        }

        // 清空映射表
        participantToEntity.clear();
    }
}