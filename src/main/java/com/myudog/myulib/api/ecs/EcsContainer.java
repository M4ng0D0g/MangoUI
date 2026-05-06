package com.myudog.myulib.api.ecs;

import com.myudog.myulib.api.events.ComponentAddedEvent;
import com.myudog.myulib.api.ecs.storage.ComponentSerializer;
import com.myudog.myulib.api.event.EventBus;
import com.myudog.myulib.api.storage.DataStorage;
import com.myudog.myulib.api.util.NbtIoHelper;
import com.myudog.myulib.internal.ecs.ComponentStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 核心 ECS 容器 (Entity-Component-System)
 * 負責管理實體生命週期、組件的高效連續儲存，以及與 NBT 系統對接的持久化存檔。
 */
public class EcsContainer {

    // ==========================================
    // 基礎設施 (Infrastructure)
    // ==========================================
    public final EventBus eventBus = new EventBus();
    private DataStorage<Integer, CompoundTag> storage;
    private final Map<Class<? extends IComponent>, ComponentSerializer<?>> serializers = new ConcurrentHashMap<>();

    // ==========================================
    // 記憶體狀態 (Runtime State)
    // ==========================================
    private int nextEntityId = 0;

    // 🌟 核心優化：使用 BitSet 以 O(1) 複雜度追蹤存活實體，極大降低 GC 壓力
    private final BitSet aliveEntities = new BitSet();

    // 組件資料庫：將不同類型的組件分類儲存於獨立的連續記憶體空間
    private final Map<Class<? extends IComponent>, ComponentStorage<? extends IComponent>> storages = new HashMap<>();

    // ==========================================
    // 生命週期與序列化 (Lifecycle & Serialization)
    // ==========================================

    /**
     * 註冊組件的序列化方式。
     */
    public <T extends IComponent> void registerSerializer(Class<T> type, ComponentSerializer<T> serializer) {
        this.serializers.put(type, serializer);
    }

    /**
     * 注入儲存實作並自動掛載伺服器生命週期。
     */
    public void install(DataStorage<Integer, CompoundTag> storageProvider) {
        this.storage = storageProvider;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (this.storage != null) {
                this.storage.initialize(server);
                Map<Integer, CompoundTag> loadedData = this.storage.loadAll();

                if (loadedData != null) {
                    loadedData.forEach(this::restoreEntity);
                    // 同步 ID 計數器，避免覆蓋已存在的實體
                    this.nextEntityId = loadedData.keySet().stream().max(Integer::compare).orElse(-1) + 1;
                }
                System.out.println("[Myulib-ECS] 容器已恢復 " + (loadedData != null ? loadedData.size() : 0) + " 個持久化實體。");
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> savePersistentEntities());
    }

    public void savePersistentEntities() {
        if (this.storage == null) return;

        for (int entityId = aliveEntities.nextSetBit(0); entityId >= 0; entityId = aliveEntities.nextSetBit(entityId + 1)) {
            CompoundTag snapshot = serializeEntity(entityId);
            if (snapshot != null && !snapshot.isEmpty()) {
                this.storage.save(entityId, snapshot);
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
    private void restoreEntity(int entityId, CompoundTag tag) {
        aliveEntities.set(entityId); // 標記為存活

        for (String className : NbtIoHelper.keysOf(tag)) {
            try {
                Class<? extends IComponent> type = (Class<? extends IComponent>) Class.forName(className);
                ComponentSerializer<?> serializer = this.serializers.get(type);
                if (serializer != null) {
                    Tag componentData = tag.get(className);
                    IComponent component = (IComponent) ((ComponentSerializer<Object>) serializer).deserialize(componentData);
                    // 恢復組件時不觸發 EventBus，避免在伺服器啟動階段引發不可控的連鎖反應
                    getStorage((Class<IComponent>) type).add(entityId, component);
                }
            } catch (Exception e) {
                System.err.println("[Myulib-ECS] 還原實體 " + entityId + " 的組件失敗: " + className);
            }
        }
    }

    // ==========================================
    // 實體管理 (Entity Management)
    // ==========================================

    public int createEntity() {
        int id = nextEntityId++;
        aliveEntities.set(id);
        return id;
    }

    public void destroyEntity(int entityId) {
        if (!aliveEntities.get(entityId)) return;

        for (ComponentStorage<? extends IComponent> s : storages.values()) {
            s.remove(entityId);
        }
        if (this.storage != null) {
            this.storage.delete(entityId);
        }
        aliveEntities.clear(entityId);
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
    }

    public <T extends IComponent> T getComponent(int entityId, Class<T> type) {
        ComponentStorage<T> storage = (ComponentStorage<T>) storages.get(type);
        return storage == null ? null : storage.get(entityId);
    }

    public <T extends IComponent> void removeComponent(int entityId, Class<T> type) {
        ComponentStorage<T> storage = (ComponentStorage<T>) storages.get(type);
        if (storage != null) {
            storage.remove(entityId);
        }
    }

    // ==========================================
    // 系統迭代器 (System Iterators)
    // ==========================================

    /**
     * 單一組件迭代 (記憶體連續存取最高效)
     */
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

    /**
     * 雙組件交集迭代 (自動以最小陣列為主迴圈優化效能)
     */
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