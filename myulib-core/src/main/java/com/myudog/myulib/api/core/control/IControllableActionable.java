package com.myudog.myulib.api.core.control;

/**
 * IControllableActionable
 *
 * 系統：核心控制系統 (Core Control System)
 * 角色：定義具備「原生動作」（跳躍、潛行、衝刺）能力的實體介面。
 * 類型：Interface / Capability
 *
 * 子類實作說明：
 * 1. 應根據 intent.type() 判斷動作類型（JUMP, SNEAK, SPRINT）。
 * 2. 應根據 intent.action() 判斷是按下 (PRESS) 還是放開 (RELEASE)。
 * 3. 實作通常涉及呼叫實體原生的 setJumping(), setShiftKeyDown() 或 setSprinting() 方法。
 */
public interface IControllableActionable extends IControllable {

    /**
     * 執行特定動作意圖。
     *
     * @param intent 包含動作類型與狀態的意圖物件
     */
    void myulib_mc$executeAction(Intent intent);
}