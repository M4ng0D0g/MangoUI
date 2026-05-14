package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.core.ecs.IComponent;

/**
 * WidgetStateComponent
 *
 * 系統：客戶端 UI 系統 (Client UI - Components)
 * 角色：儲存 UI 節點的互動狀態 (顯示、啟用、懸停、按下、焦點)。
 * 類型：ECS Component
 *
 * 此組件決定了 UI 節點的渲染表現與互動行為。
 * 例如：未啟用 (enabled=false) 的節點通常不接收點擊事件，不顯示 (visible=false) 的節點則跳過渲染。
 */
public class WidgetStateComponent implements IComponent {
    /** 是否顯示。 */
    public boolean visible = true;
    /** 是否啟用互動。 */
    public boolean enabled = true;
    /** 滑鼠是否懸停在上方。 */
    public boolean hovered;
    /** 是否正在被按下。 */
    public boolean pressed;
    /** 是否擁有焦點。 */
    public boolean focused;
}
