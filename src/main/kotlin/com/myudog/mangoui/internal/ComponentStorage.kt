package com.myudog.mangoui.internal

import com.myudog.mangoui.api.Component

/**
 * [Internal] 高性能組件存儲。
 * 使用 Sparse Set 結構，確保遍歷時 CPU 快取友善 (Cache Friendly)。
 */
@Suppress("UNCHECKED_CAST")
internal class ComponentStorage<T : Component>(initialCapacity: Int = 1024) {
    // 密集陣列：實際儲存組件實例，內存連續
    private var components = arrayOfNulls<Component>(initialCapacity) as Array<T?>
    // 密集索引：儲存該位置對應的 Widget ID
    private var dense = IntArray(initialCapacity) { -1 }
    // 稀疏索引：索引是 Widget ID，值是 dense 陣列的下標
    private var sparse = IntArray(initialCapacity * 10) { -1 }

    var size = 0
        private set

    /**
     * 新增或更新組件數據
     */
    fun add(entityId: Int, component: T) {
        ensureSparseCapacity(entityId)

        if (sparse[entityId] == -1) {
            // 新增邏輯
            ensureDenseCapacity(size)
            sparse[entityId] = size
            dense[size] = entityId
            components[size] = component
            size++
        } else {
            // 更新邏輯：直接覆蓋連續內存中的舊數據
            components[sparse[entityId]] = component
        }
    }

    /**
     * O(1) 取得組件數據
     */
    fun get(entityId: Int): T? {
        val index = if (entityId < sparse.size) sparse[entityId] else -1
        return if (index != -1) components[index] else null
    }

    /**
     * O(1) 移除組件
     */
    fun remove(entityId: Int) {
        val index = if (entityId < sparse.size) sparse[entityId] else -1
        if (index == -1) return

        // 將最後一個元素移到被刪除的位置，維持 dense 陣列的連續性
        val lastEntityId = dense[size - 1]
        components[index] = components[size - 1]
        dense[index] = lastEntityId
        sparse[lastEntityId] = index

        // 清理殘餘數據
        sparse[entityId] = -1
        components[size - 1] = null
        size--
    }

    private fun ensureSparseCapacity(id: Int) {
        if (id >= sparse.size) sparse = sparse.copyOf(id * 2 + 1)
    }

    private fun ensureDenseCapacity(s: Int) {
        if (s >= components.size) {
            val newSize = s * 2
            components = components.copyOf(newSize) as Array<T?>
            dense = dense.copyOf(newSize)
        }
    }
}