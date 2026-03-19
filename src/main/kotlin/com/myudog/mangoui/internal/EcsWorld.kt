package com.myudog.mangoui.internal

import com.myudog.mangoui.api.Component
import java.util.concurrent.atomic.AtomicInteger

/**
 * [Internal] UI 數據中樞
 * 這裡的 Entity 僅代表「UI 元件的唯一識別碼」。
 */
internal class EcsWorld {
    private val nextWidgetId = AtomicInteger(0)
    private val storages = mutableMapOf<Class<out Component>, ComponentStorage<out Component>>()

    /**
     * 分配一個新的 UI 元件 ID
     */
    fun createEntity(): Int = nextWidgetId.getAndIncrement()

    /**
     * 為 UI 元件掛載屬性 (如 Transform, Style)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> addComponent(widgetId: Int, component: T) {
        val type = component.javaClass
        val storage = storages.getOrPut(type) {
            ComponentStorage<T>()
        } as ComponentStorage<T>
        storage.add(widgetId, component)
    }

    fun <T : Component> getStorage(type: Class<T>): ComponentStorage<T>? {
        return storages[type] as? ComponentStorage<T>
    }
}