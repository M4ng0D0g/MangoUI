package com.myudog.myulib.internal.ecs;

import com.myudog.myulib.api.core.ecs.IComponent;

/**
 * ComponentStorage
 *
 * 系統：核心 ECS - 內部儲存層
 * 角色：使用 Sparse Set (稀疏集) 資料結構實現組件的高效連續儲存。
 * 類型：Data Structure / Internal Storage
 *
 * 透過 Dense Array 保持資料的記憶體連續性，從而極大化 CPU Cache Hit Rate；
 * 同時透過 Sparse Array 提供 O(1) 的實體 ID 到組件索引的映射。
 *
 * @param <T> 組件的類型
 */
public class ComponentStorage<T extends IComponent> {
    private Object[] components;
    private int[] dense;
    private int[] sparse;
    private int size;

    /**
     * 以預設容量 (1024) 建立組件儲存空間。
     */
    public ComponentStorage() {
        this(1024);
    }

    /**
     * 以指定容量建立組件儲存空間。
     *
     * @param initialCapacity 初始實體 ID 空間容量
     */
    public ComponentStorage(int initialCapacity) {
        int capacity = Math.max(16, initialCapacity);
        this.components = new Object[capacity];
        this.dense = new int[capacity];
        this.sparse = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            sparse[i] = -1;
        }
    }

    /**
     * 獲取目前儲存的組件數量。
     *
     * @return 組件總數
     */
    public int size() {
        return size;
    }

    /**
     * 獲取原始的 Dense Array (實體 ID 列表)。
     * 此陣列的前 size 個元素是有效的實體 ID，用於高效迭代。
     *
     * @return 原始實體 ID 陣列
     */
    public int[] getRawDense() {
        return dense;
    }

    /**
     * 檢查特定實體是否擁有此類型的組件。
     *
     * @param entityId 目標實體 ID
     * @return 若擁有組件則為 true
     */
    public boolean has(int entityId) {
        if (entityId < 0 || entityId >= sparse.length) {
            return false;
        }
        int index = sparse[entityId];
        return index >= 0 && index < size && dense[index] == entityId;
    }

    /**
     * 新增或更新實體的組件。
     *
     * @param entityId  目標實體 ID
     * @param component 要掛載的組件實例
     */
    public void add(int entityId, T component) {
        ensureSparseCapacity(entityId);
        int index = sparse[entityId];
        if (index < 0 || index >= size || dense[index] != entityId) {
            ensureDenseCapacity(size + 1);
            sparse[entityId] = size;
            dense[size] = entityId;
            components[size] = component;
            size++;
        } else {
            components[index] = component;
        }
    }

    /**
     * 獲取指定實體的組件實例。
     *
     * @param entityId 目標實體 ID
     * @return 組件實例，若不存在則為 null
     */
    @SuppressWarnings("unchecked")
    public T get(int entityId) {
        if (!has(entityId)) {
            return null;
        }
        return (T) components[sparse[entityId]];
    }

    /**
     * 移除指定實體的組件。
     * 內部會將 Dense Array 的最後一個元素移動到被移除的位置，以維持記憶體連續性。
     *
     * @param entityId 目標實體 ID
     */
    public void remove(int entityId) {
        if (!has(entityId)) {
            return;
        }

        int indexToRemove = sparse[entityId];
        int lastIndex = size - 1;
        int lastEntityId = dense[lastIndex];

        components[indexToRemove] = components[lastIndex];
        dense[indexToRemove] = lastEntityId;
        sparse[lastEntityId] = indexToRemove;

        components[lastIndex] = null;
        sparse[entityId] = -1;
        size--;
    }

    /**
     * 清空所有儲存的組件。
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            components[i] = null;
            sparse[dense[i]] = -1;
        }
        size = 0;
    }

    private void ensureSparseCapacity(int entityId) {
        if (entityId < sparse.length) {
            return;
        }
        int newSize = Math.max(entityId + 1, sparse.length * 2);
        int[] newSparse = new int[newSize];
        for (int i = 0; i < newSize; i++) {
            newSparse[i] = -1;
        }
        System.arraycopy(sparse, 0, newSparse, 0, sparse.length);
        sparse = newSparse;
    }

    private void ensureDenseCapacity(int targetSize) {
        if (targetSize <= components.length) {
            return;
        }
        int newSize = components.length * 2;
        Object[] newComponents = new Object[newSize];
        int[] newDense = new int[newSize];
        System.arraycopy(components, 0, newComponents, 0, components.length);
        System.arraycopy(dense, 0, newDense, 0, dense.length);
        components = newComponents;
        dense = newDense;
    }
}
