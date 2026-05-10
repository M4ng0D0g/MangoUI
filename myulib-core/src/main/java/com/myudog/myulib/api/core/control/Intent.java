package com.myudog.myulib.api.core.control;

import net.minecraft.world.phys.Vec3;

public record Intent(IntentType type, Vec3 vector, int keyCode, String customAction) {

    /** * 向量移動指令
     */
    public static Intent move(Vec3 vector) {
        return new Intent(IntentType.MOVE_VECTOR, vector, 0, null);
    }

    /** * 基礎按鍵/動作指令 (例如跳躍、左鍵、右鍵)
     */
    public static Intent action(IntentType type) {
        return new Intent(type, Vec3.ZERO, 0, null);
    }

    /** * 通用擴充按鍵指令 (傳遞具體 KeyCode)
     */
    public static Intent generic(int keyCode) {
        return new Intent(IntentType.GENERIC_ACTION, Vec3.ZERO, keyCode, null);
    }

    /** * 自定義字串動作指令 (例如觸發特定技能)
     */
    public static Intent custom(String action) {
        return new Intent(IntentType.GENERIC_ACTION, Vec3.ZERO, 0, action);
    }
}