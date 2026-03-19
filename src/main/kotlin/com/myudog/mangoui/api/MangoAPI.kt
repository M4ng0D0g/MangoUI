package com.myudog.mangoui.api

import com.myudog.mangoui.internal.EcsWorld
import com.myudog.mangoui.MangoUI

/**
 * [API] Mango UI 的全域訪問入口。
 * 所有對底層 ECS 的操作都應該透過此類別進行。
 */
object MangoAPI {

    /**
     * 建立一個新的 UI 實體。
     */
    fun createEntity(): Int {
        return MangoUI.internalWorld.createEntity()
    }

    /**
     * [核心] 為實體添加組件。
     */
    fun <T : Component> addComponent(entityId: Int, component: T) {
        MangoUI.internalWorld.addComponent(entityId, component)
    }

    /**
     * [核心] 獲取實體的組件（使用 Kotlin 泛型優化）。
     * 使用範例：val transform = MangoAPI.getComponent<TransformComponent>(id)
     */
    inline fun <reified T : Component> getComponent(entityId: Int): T? {
        return getComponent(entityId, T::class.java)
    }

    /**
     * [方式 2] 通用型：傳入 Class 物件
     * 呼叫範例：getComponent(id, TransformComponent::class.java)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> getComponent(entityId: Int, type: Class<T>): T? {
        return MangoUI.internalWorld.getStorage(type)?.get(entityId) as? T
    }

    /**
     * 檢查實體是否擁有特定組件。
     */
    inline fun <reified T : Component> hasComponent(entityId: Int): Boolean {
        return getComponent<T>(entityId) != null
    }

    /**
     * 請求全域重新佈局。
         * 當你手動修改了 Transform 但沒有觸發自動更新時使用。
     */
    fun requestLayout() {
        // 未來這裡會呼叫 LayoutSystem.markDirty()
        MangoUI.LOGGER.debug("Layout update requested.")
    }

    /**
     * 獲取底層世界的唯讀引用（僅供進階擴充使用）。
     */
    internal fun getInternalWorld(): EcsWorld = MangoUI.internalWorld
}