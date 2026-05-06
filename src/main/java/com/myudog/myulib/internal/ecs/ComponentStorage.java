package com.myudog.myulib.internal.ecs;

import com.myudog.myulib.api.ecs.IComponent;

public class ComponentStorage<T extends IComponent> {
    private Object[] components;
    private int[] dense;
    private int[] sparse;
    private int size;

    public ComponentStorage() {
        this(1024);
    }

    public ComponentStorage(int initialCapacity) {
        int capacity = Math.max(16, initialCapacity);
        this.components = new Object[capacity];
        this.dense = new int[capacity];
        this.sparse = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            sparse[i] = -1;
        }
    }

    public int size() {
        return size;
    }

    public int[] getRawDense() {
        return dense;
    }

    public boolean has(int entityId) {
        if (entityId < 0 || entityId >= sparse.length) {
            return false;
        }
        int index = sparse[entityId];
        return index >= 0 && index < size && dense[index] == entityId;
    }

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

    @SuppressWarnings("unchecked")
    public T get(int entityId) {
        if (!has(entityId)) {
            return null;
        }
        return (T) components[sparse[entityId]];
    }

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
