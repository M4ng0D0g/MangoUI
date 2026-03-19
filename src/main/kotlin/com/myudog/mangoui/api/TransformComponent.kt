package com.myudog.mangoui.api

/**
 * [API] 變換組件：定義 UI 元件的佈局規則。
 * 這裡不儲存最終的像素座標，而是儲存「如何計算座標」的意圖。
 */
data class TransformComponent(
    var anchor: Anchor = Anchor.TOP_LEFT,
    var width: SizeUnit = SizeUnit.Fixed(100f),
    var height: SizeUnit = SizeUnit.Fixed(20f),
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    // 內距與外距預留
    var padding: BoxValues = BoxValues.ZERO,
    var margin: BoxValues = BoxValues.ZERO
) : Component

/**
 * 錨點：決定元件相對於父容器的對齊位置
 */
enum class Anchor {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

/**
 * 尺寸單位：支援固定像素、父容器百分比、填滿或包裹內容
 */
sealed class SizeUnit {
    data class Fixed(val px: Float) : SizeUnit()
    data class Relative(val percent: Float) : SizeUnit() // 0.0 ~ 1.0
    object FillContainer : SizeUnit()
    object WrapContent : SizeUnit()
}

/**
 * 用於儲存上下左右的數值（Padding/Margin）
 */
data class BoxValues(val top: Float, val bottom: Float, val left: Float, val right: Float) {
    companion object {
        val ZERO = BoxValues(0f, 0f, 0f, 0f)
    }
}