package com.myudog.myulib.api.core.camera;

public enum CameraPerspective {
    /** 自由模式：完全不干涉，允許玩家自由使用 F5 切換 */
    FREE,
    
    /** 強制第一人稱 */
    FIRST_PERSON,
    
    /** 強制第三人稱背面 (原版 F5 第一下) */
    THIRD_PERSON_BACK,
    
    /** 強制第三人稱正面 (原版 F5 第二下) */
    THIRD_PERSON_FRONT,
    
    /** * 特定偏移模式：
     * 不使用原版的置中視角，而是根據 CameraTrackingTarget 提供的 offset 進行絕對偏移
     */
    CUSTOM_OFFSET;

    /** 判斷此模式是否為強制鎖定人稱 (非 FREE 狀態) */
    public boolean isForced() {
        return this != FREE;
    }
}