package com.myudog.myulib.api.core.control;

public interface IControllableActionable extends IControllable {
    /** * 處理跳躍、潛行、衝刺等單次或持續性動作意圖
     * 具體實作通常是修改實體的 jumping, sneaking, sprinting 標籤
     */
    void executeAction(Intent intent);
}