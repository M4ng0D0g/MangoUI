package com.myudog.mangoui.api

/**
 * [API] 所有 UI 元件的基類。
 * 它不儲存數據，而是代理對 ECS 實體的訪問。
 */
abstract class BaseWidget(val entityId: Int = MangoAPI.createEntity()) {

    init {
        // 每個 Widget 誕生時，預設一定要有變換與層級組件
        MangoAPI.addComponent(entityId, TransformComponent())
        MangoAPI.addComponent(entityId, HierarchyComponent())
    }

    // 快捷存取 Transform
    val transform: TransformComponent
        get() = MangoAPI.getComponent(entityId, TransformComponent::class.java)!!

    // 快捷存取 Hierarchy
    protected val hierarchy: HierarchyComponent
        get() = MangoAPI.getComponent(entityId, HierarchyComponent::class.java)!!

    /**
     * 將此組件掛載到另一個父組件下
     */
    fun setParent(parent: BaseWidget?) {
        val oldParentId = hierarchy.parent

        // 從舊父組件移除
        oldParentId?.let { pid ->
            MangoAPI.getComponent(pid, HierarchyComponent::class.java)?.children?.remove(entityId)
        }

        // 設置新父組件
        hierarchy.parent = parent?.entityId
        parent?.hierarchy?.children?.add(entityId)
    }

    /**
     * 當組件被加到父容器時觸發
     */
    open fun onAdded() {}

    /**
     * 當組件被移除時觸發
     */
    open fun onRemoved() {}

    /**
     * 每一幀的邏輯更新 (非渲染)
     */
    open fun onTick() {}
}