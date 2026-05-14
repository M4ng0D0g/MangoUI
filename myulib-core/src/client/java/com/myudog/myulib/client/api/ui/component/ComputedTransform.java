package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.core.ecs.IComponent;

/**
 * ComputedTransform
 *
 * 系統：客戶端 UI 系統 (Client UI - Components)
 * 角色：儲存經過佈局計算後，在螢幕空間的最終絕對變換資訊。
 * 類型：ECS Component / Computed Data
 *
 * 不同於 {@link TransformComponent} 儲存的是相對位置與原始設定，
 * `ComputedTransform` 由佈局系統 (Layout System) 在每一幀或資料變更時計算產出，
 * 供渲染系統 (Render System) 直接使用於繪圖。
 */
public class ComputedTransform implements IComponent {
    /** 最終螢幕絕對 X 座標。 */
    public float x;
    /** 最終螢幕絕對 Y 座標。 */
    public float y;
    /** 計算後的最終寬度。 */
    public float width;
    /** 計算後的最終高度。 */
    public float height;
    /** 最終累計縮放 X。 */
    public float scaleX = 1.0f;
    /** 最終累計縮放 Y。 */
    public float scaleY = 1.0f;
    /** 最終累計旋轉。 */
    public float rotation;
    /** 最終累計不透明度。 */
    public float opacity = 1.0f;
}
