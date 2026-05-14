package com.myudog.myulib.internal.game;

import com.myudog.myulib.api.framework.game.core.GameDefinition;
import com.myudog.myulib.api.framework.game.core.GameInstance;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameRegistry {
    // 遊戲定義註冊表
    private final Map<Identifier, GameDefinition<?, ?, ?>> definitions = new LinkedHashMap<>();

    // 運行中的實例 (ID -> Instance)
    private final Map<String, GameInstance<?, ?, ?>> instances = new ConcurrentHashMap<>();

    // 映射管理 (UUID -> Instance ID)
    private final Map<UUID, String> playerToInstanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityToInstanceMap = new ConcurrentHashMap<>();

    // --- Definition 管理 ---

    public void registerDefinition(@NotNull GameDefinition<?, ?, ?> definition) {
        definitions.put(definition.id(), definition);
    }

    @Nullable
    public GameDefinition<?, ?, ?> getDefinition(@NotNull Identifier id) {
        return definitions.get(id);
    }

    // --- Instance 管理 ---

    public void addInstance(@NotNull GameInstance<?, ?, ?> instance) {
        instances.put(instance.getInstanceId(), instance);
    }

    @Nullable
    public GameInstance<?, ?, ?> removeInstance(@NotNull String instanceId) {
        return instances.remove(instanceId);
    }

    @Nullable
    public GameInstance<?, ?, ?> getInstance(@NotNull String instanceId) {
        return instances.get(instanceId);
    }

    public Collection<GameInstance<?, ?, ?>> instances() {
        return instances.values();
    }

    // --- 映射管理 ---

    public void linkPlayer(@NotNull UUID uuid, @NotNull String instanceId) {
        playerToInstanceMap.put(uuid, instanceId);
    }

    public void linkEntity(@NotNull UUID entityUuid, @NotNull String instanceId) {
        entityToInstanceMap.put(entityUuid, instanceId);
    }

    public void unlinkPlayer(@NotNull UUID uuid) {
        playerToInstanceMap.remove(uuid);
    }

    public void unlinkEntity(@NotNull UUID entityUuid) {
        entityToInstanceMap.remove(entityUuid);
    }

    @Nullable
    public GameInstance<?, ?, ?> getInstanceByPlayer(UUID uuid) {
        String id = playerToInstanceMap.get(uuid);
        return id != null ? instances.get(id) : null;
    }

    @Nullable
    public GameInstance<?, ?, ?> getInstanceByEntity(@NotNull UUID entityUuid) {
        String instanceId = entityToInstanceMap.get(entityUuid);
        return instanceId != null ? instances.get(instanceId) : null;
    }

    public void clearMappingsForInstance(@NotNull String instanceId) {
        playerToInstanceMap.entrySet().removeIf(entry -> entry.getValue().equals(instanceId));
        entityToInstanceMap.entrySet().removeIf(entry -> entry.getValue().equals(instanceId));
    }

    public void clearAll() {
        instances.clear();
        playerToInstanceMap.clear();
        entityToInstanceMap.clear();
    }

    public Collection<GameDefinition<?, ?, ?>> definitions() {
        return definitions.values();
    }
}