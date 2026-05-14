package com.myudog.myulib.api.framework.game.features;

import com.myudog.myulib.api.core.ecs.EcsContainer;
import com.myudog.myulib.api.framework.game.core.GameInstance;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EcsFeatureImpl implements EcsFeature {

    // ?萄? Java ?賢?閬?嚗祕靘??訾蝙?典?擏陸
    private final EcsContainer container = new EcsContainer();

    // ?? ?詨???嚗蝙??ConcurrentHashMap 蝣箔??拙振銝衣?/??箸??銵?摰
    private final Map<UUID, Integer> participantToEntity = new ConcurrentHashMap<>();

    public EcsFeatureImpl() {}

    @Override
    public EcsContainer getContainer() {
        return container;
    }

    @Override
    public Optional<Integer> getEntity(@NotNull UUID uuid) {
        // ?? ??芸?嚗??containsKey + get ?甈⊥?橘??湔??銝血?鋆?
        return Optional.ofNullable(participantToEntity.get(uuid));
    }

    @Override
    public int getOrCreateParticipant(@NotNull UUID uuid) {
        // ?? ????嚗????府 UUID嚗????container.createEntity()嚗蒂靽??瑁?蝺???
        return participantToEntity.computeIfAbsent(uuid, k -> container.createEntity());
    }

    @Override
    public int removeParticipant(@NotNull UUID uuid) {
        // ?湔蝘駁銝衣???潘??踹?憭活?亥岷
        Integer entityId = participantToEntity.remove(uuid);

        if (entityId == null) {
            return -1;
        }

        // 蝣箏祕?瑟?摨惜 ECS 摰孵銝剔?撖阡?
        container.destroyEntity(entityId);
        return entityId;
    }

    @Override
    public void clean(GameInstance<?, ?, ?> instance) {
        // ?? 撖虫?皜??摩嚗瘥???摰??拙振撖阡?
        for (Integer entityId : participantToEntity.values()) {
            container.destroyEntity(entityId);
        }

        // 皜征??銵?
        participantToEntity.clear();
    }
}
