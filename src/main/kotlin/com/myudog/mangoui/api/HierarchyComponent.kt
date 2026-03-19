package com.myudog.mangoui.api

/**
 * [API] 層級組件：定義 UI 樹狀結構。
 */
data class HierarchyComponent(
    var parent: Int? = null,
    val children: MutableList<Int> = mutableListOf()
) : Component