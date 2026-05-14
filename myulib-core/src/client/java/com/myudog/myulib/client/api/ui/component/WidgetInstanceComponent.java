package com.myudog.myulib.client.api.ui.component;

import com.myudog.myulib.api.core.ecs.IComponent;

/**
 * WidgetInstanceComponent
 *
 * 系統：客戶端 UI 系統 (Client UI - Components)
 * 角色：連結 ECS 實體與具體的 UI 渲染物件 (Widget)。
 * 類型：ECS Component
 *
 * 此組件儲存了 UI 框架中對應的 Widget 實例引用。
 * `dirty` 旗標用於通知渲染系統該 Widget 的資料或變換已發生改變，需要重新構建或更新。
 */
public class WidgetInstanceComponent implements IComponent {
    /** Widget 的唯一識別碼。 */
    public String widgetId;
    /** 對應的 UI 實例物件 (如 Minecraft 原生 Widget 或自定義組件)。 */
    public Object widget;
    /** 髒標記，為 true 時代表需要更新。 */
    public boolean dirty = true;
}
