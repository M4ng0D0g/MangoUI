package com.myudog.myulib.api.core.ecs;

import com.myudog.myulib.api.core.ecs.UuidComponent;
import com.myudog.myulib.api.core.events.ComponentAddedEvent;
import com.myudog.myulib.api.core.ecs.storage.ComponentSerializer;
import com.myudog.myulib.api.core.event.EventBus;
import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.api.core.storage.DataStorage;
import com.myudog.myulib.api.core.util.NbtIoHelper;
import com.myudog.myulib.internal.ecs.ComponentStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * EcsContainer
 *
 * 系統：核心 ECS (Entity-Component-System)
 * 角色：ECS 系統的中央管理器，負責實體的生命週期、組件的高效存取與持久化儲存。
 * 類型：Manager / Container
 *
 * 此類別採用了連續記憶體佈局 (Data-Oriented Design) 的優化思想，透過 ComponentStorage 實現高效的組件迭代，
 * 並整合了基於 UUID 的持久化機制，確保實體資料能在伺服器重啟後穩定恢復。
 */
public class EcsContainer {

    // ==========================================
    // 基礎設施 (Infrastructure)
    // ==========================================
    public final EventBus eventBus = new EventBus();
    private DataStorage<UUID, CompoundTag> storage;
    private final Map<Class<? extends IComponent>, ComponentSerializer<?>> serializers = new ConcurrentHashMap<>();

    // ==========================================
    // 記憶體狀態 (Runtime State)
    // ==========================================
    private int nextEntityId = 0;
    private final BitSet aliveEntities = new BitSet();
    private final Map<UUID, Integer> uuidToId = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> idToUuid = new ConcurrentHashMap<>();

    // 組件資料庫：將不同類型的組件分類儲存於獨立的連續記憶體空間
    private final Map<Class<? extends IComponent>, ComponentStorage<? extends IComponent>> storages = new HashMap<>();

    // ==========================================
    // 生命週期與序列化 (Lifecycle & Serialization)
    // ==========================================

    public <T extends IComponent> void registerSerializer(Class<T> type, ComponentSerializer<T> serializer) {
        this.serializers.put(type, serializer);
    }

    public void install(DataStorage<UUID, CompoundTag> storageProvider) {
        this.storage = storageProvider;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (this.storage != null) {
                this.storage.initialize(server);
                Map<UUID, CompoundTag> loadedData = this.storage.loadAll();

                if (loadedData != null) {
                    loadedData.forEach(this::restoreEntity);
                }
                DebugLogManager.INSTANCE.log(DebugFeature.ECS,
                        "restore: entities=" + (loadedData != null ? loadedData.size() : 0));
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> savePersistentEntities());
    }

    public void savePersistentEntities() {
        if (this.storage == null) return;

        for (int entityId = aliveEntities.nextSetBit(0); entityId >= 0; entityId = aliveEntities.nextSetBit(entityId + 1)) {
            UUID uuid = idToUuid.get(entityId);
            if (uuid == null) continue;

            CompoundTag snapshot = serializeEntity(entityId);
            if (snapshot != null && !snapshot.isEmpty()) {
                this.storage.save(uuid, snapshot);
            }
        }
    }

    private CompoundTag serializeEntity(int entityId) {
        CompoundTag entityTag = new CompoundTag();
        boolean hasData = false;

        for (Map.Entry<Class<? extends IComponent>, ComponentStorage<? extends IComponent>> entry : storages.entrySet()) {
            IComponent component = entry.getValue().get(entityId);
            if (component == null) continue;

            @SuppressWarnings("unchecked")
            ComponentSerializer<IComponent> serializer = (ComponentSerializer<IComponent>) this.serializers.get(entry.getKey());
            if (serializer != null) {
                entityTag.put(entry.getKey().getName(), serializer.serialize(component));
                hasData = true;
            }
        }
        return hasData ? entityTag : null;
    }

    @SuppressWarnings("unchecked")
    private void restoreEntity(UUID uuid, CompoundTag tag) {
        int entityId = createEntity(uuid);

        for (String className : NbtIoHelper.keysOf(tag)) {
            try {
                Class<? extends IComponent> type = (Class<? extends IComponent>) Class.forName(className);
                ComponentSerializer<?> serializer = this.serializers.get(type);
                if (serializer != null) {
                    Tag componentData = tag.get(className);
                    IComponent component = (IComponent) ((ComponentSerializer<Object>) serializer).deserialize(componentData);
                    getStorage((Class<IComponent>) type).add(entityId, component);
                }
            } catch (Exception e) {
                System.err.println("[Myulib-ECS] 還原實體 " + uuid + " 的組件失敗: " + className);
            }
        }
    }

    // ==========================================
    // 實體管理 (Entity Management)
    // ==========================================

    public int createEntity() {
        return createEntity(null);
    }

    public int createEntity(UUID uuid) {
        int id = nextEntityId++;
        aliveEntities.set(id);
        
        if (uuid != null) {
            uuidToId.put(uuid, id);
            idToUuid.put(id, uuid);
            addComponent(id, UuidComponent.class, new UuidComponent(uuid));
        }

        DebugLogManager.INSTANCE.log(DebugFeature.ECS, "create entity=" + id + (uuid != null ? " (uuid=" + uuid + ")" : ""));
        return id;
    }

    public void destroyEntity(int entityId) {
        if (!aliveEntities.get(entityId)) return;

        for (ComponentStorage<? extends IComponent> s : storages.values()) {
            s.remove(entityId);
        }
        
        UUID uuid = idToUuid.remove(entityId);
        if (uuid != null) {
            uuidToId.remove(uuid);
            if (this.storage != null) {
                this.storage.delete(uuid);
            }
        }
        
        aliveEntities.clear(entityId);
        DebugLogManager.INSTANCE.log(DebugFeature.ECS, "destroy entity=" + entityId);
    }

    public Integer getEntityId(UUID uuid) {
        return uuidToId.get(uuid);
    }

    public UUID getUuid(int entityId) {
        return idToUuid.get(entityId);
    }

    public boolean hasEntity(int entityId) {
        return aliveEntities.get(entityId);
    }

    // ==========================================
    // 組件管理 (Component Management)
    // ==========================================

    @SuppressWarnings("unchecked")
    public <T extends IComponent> ComponentStorage<T> getStorage(Class<T> type) {
        return (ComponentStorage<T>) storages.computeIfAbsent(type, key -> new ComponentStorage<>());
    }

    public <T extends IComponent> void addComponent(int entityId, Class<T> type, T component) {
        getStorage(type).add(entityId, component);
        eventBus.dispatch(new ComponentAddedEvent(entityId, component));
        
        if (component instanceof UuidComponent uuidComp) {
            UUID uuid = uuidComp.uuid();
            uuidToId.put(uuid, entityId);
            idToUuid.put(entityId, uuid);
        }
    }

    public <T extends IComponent> T getComponent(int entityId, Class<T> type) {
        ComponentStorage<T> storage = (ComponentStorage<T>) storages.get(type);
        return storage == null ? null : storage.get(entityId);
    }

    public <T extends IComponent> void removeComponent(int entityId, Class<T> type) {
        ComponentStorage<T> storage = (ComponentStorage<T>) storages.get(type);
        if (storage != null) {
            IComponent comp = storage.get(entityId);
            if (comp instanceof UuidComponent uuidComp) {
                UUID uuid = uuidComp.uuid();
                uuidToId.remove(uuid);
                idToUuid.remove(entityId);
            }
            storage.remove(entityId);
        }
    }

    // ==========================================
    // 系統迭代器 (System Iterators)
    // ==========================================

    public <T extends IComponent> void forAll(Class<T> type, BiConsumer<Integer, T> action) {
        ComponentStorage<T> storage = getStorage(type);
        int[] dense = storage.getRawDense();
        int size = storage.size();

        for (int i = 0; i < size; i++) {
            int entityId = dense[i];
            if (aliveEntities.get(entityId)) {
                action.accept(entityId, storage.get(entityId));
            }
        }
    }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    public <T1 extends IComponent, T2 extends IComponent> void forAll(
            Class<T1> type1, Class<T2> type2, TriConsumer<Integer, T1, T2> action) {

        ComponentStorage<T1> storage1 = getStorage(type1);
        ComponentStorage<T2> storage2 = getStorage(type2);

        ComponentStorage<?> smallest = storage1.size() <= storage2.size() ? storage1 : storage2;
        int[] dense = smallest.getRawDense();
        int size = smallest.size();

        for (int i = 0; i < size; i++) {
            int entityId = dense[i];
            if (!aliveEntities.get(entityId)) continue;

            T1 c1 = storage1.get(entityId);
            T2 c2 = storage2.get(entityId);

            if (c1 != null && c2 != null) {
                action.accept(entityId, c1, c2);
            }
        }
    }
}