package com.myudog.myulib.api.core.control;

import net.minecraft.world.phys.Vec3;

/**
 * 玩家意圖封裝類 (Intent)
 * <p>
 * 整合了動作類型、向量數據、按鍵行為 (PRESS/RELEASE) 以及時間戳記。
 * 支援伺服器端進行連續指令偵測 (Input Sequence Detection)。
 */
public record Intent(
        IntentType type,
        InputAction action,
        Vec3 vector,
        int keyCode,
        String customAction,
        long timestamp
) {

    /**
     * 向量移動指令 (通常對應 InputAction.MOVE)
     */
    public static Intent move(Vec3 vector) {
        return new Intent(IntentType.MOVE_VECTOR, InputAction.MOVE, vector, 0, null, System.currentTimeMillis());
    }

    /**
     * 基礎動作指令 (預設為 PRESS)
     */
    public static Intent action(IntentType type, InputAction action) {
        return new Intent(type, action, Vec3.ZERO, 0, null, System.currentTimeMillis());
    }

    /**
     * 通用擴充按鍵指令
     */
    public static Intent generic(int keyCode, InputAction action) {
        return new Intent(IntentType.GENERIC_ACTION, action, Vec3.ZERO, keyCode, null, System.currentTimeMillis());
    }

    /**
     * 自定義字串動作指令
     */
    public static Intent custom(String action, InputAction inputAction) {
        return new Intent(IntentType.GENERIC_ACTION, inputAction, Vec3.ZERO, 0, action, System.currentTimeMillis());
    }

    /**
     * 完整構造函數的便捷封裝
     */
    public static Intent of(IntentType type, InputAction action, Vec3 vector, int keyCode, String customAction) {
        return new Intent(type, action, vector, keyCode, customAction, System.currentTimeMillis());
    }
}