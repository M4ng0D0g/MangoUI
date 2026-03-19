package com.myudog.mangoui.api

/**
 * [API] 面板元件：作為其他元件的容器。
 */
open class Panel : BaseWidget() {

    /**
     * 加入子元件到面板中
     */
    fun addChild(child: BaseWidget) {
        child.setParent(this)
    }

    /**
     * 移除特定子元件
     */
    fun removeChild(child: BaseWidget) {
        if (child.entityId in hierarchy.children) {
            child.setParent(null)
        }
    }
}