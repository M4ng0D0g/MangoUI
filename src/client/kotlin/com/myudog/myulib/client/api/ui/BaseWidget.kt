package com.myudog.myulib.client.api.ui

import com.myudog.myulib.client.api.MyulibApiClient
import com.myudog.myulib.client.api.ui.component.*
import net.minecraft.client.gui.DrawContext

/**
 * [API] UI 最底層基類。
 * 僅包含所有元件共有的基礎數據：Transform, Hierarchy, ItemLayout, Computed。
 */
abstract class BaseWidget(val entityId: Int = MyulibApiClient.createEntity()) {

    /**
     * Developer testing flag. Set to true when this widget has been tested locally.
     * The RenderSystem will draw a small badge for tested widgets so developers
     * can visually confirm coverage during manual testing.
     */
    var tested: Boolean = false

    init {
        // 每個 Widget 自身都具備變換、層級、以及「身為一個元件」的佈局屬性 (Margin/Padding/Weight)
        MyulibApiClient.addComponent(entityId, TransformComponent())
        MyulibApiClient.addComponent(entityId, HierarchyComponent())
        MyulibApiClient.addComponent(entityId, FlexItemComponent()) // 取代舊的 LayoutComponent
        MyulibApiClient.addComponent(entityId, ComputedTransform())
        // 每個 Widget 皆有一個狀態組件，供渲染/互動系統使用
        MyulibApiClient.addComponent(entityId, WidgetStateComponent())

        onInit()
    }

    // --- 屬性代理 (Property Delegates) ---
    val transform: TransformComponent get() = MyulibApiClient.getComponent(entityId)!!
    val flexItem: FlexItemComponent get() = MyulibApiClient.getComponent(entityId)!! // 自身盒子模型
    val computed: ComputedTransform get() = MyulibApiClient.getComponent(entityId)!!
    protected val hierarchy: HierarchyComponent get() = MyulibApiClient.getComponent(entityId)!!
    // Provide public access to state for cases where external controllers need to toggle visibility
    val state: com.myudog.myulib.client.api.ui.component.WidgetStateComponent get() = MyulibApiClient.getComponent(entityId)!!

    // Provide item-level configuration used by nodes (padding/weight)
    val itemConfig: com.myudog.myulib.client.api.ui.ItemConfig get() = com.myudog.myulib.client.api.ui.ItemConfig()

    // --- 生命週期 ---
    open fun onInit() {}
    open fun draw(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {}

    /**
     * 基礎層級操作
     */
    fun setParent(parent: BaseWidget?) {
        val oldParentId = hierarchy.parent
        // 從舊父元件中移除自己
        oldParentId?.let { pid ->
            MyulibApiClient.getComponent<HierarchyComponent>(pid)?.children?.remove(entityId)
        }
        // 設定新父元件
        hierarchy.parent = parent?.entityId
        parent?.let {
            val parentHierarchy = MyulibApiClient.getComponent<HierarchyComponent>(it.entityId)
            if (parentHierarchy?.children?.contains(entityId) == false) {
                parentHierarchy.children.add(entityId)
            }
        }
    }
}