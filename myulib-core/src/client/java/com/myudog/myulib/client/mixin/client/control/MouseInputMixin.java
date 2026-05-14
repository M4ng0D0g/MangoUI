package com.myudog.myulib.client.mixin.client.control;

import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.api.core.control.InputAction;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.api.core.control.IntentType;
import com.myudog.myulib.client.api.control.ClientControlManager;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 控制系統：滑鼠輸入攔截與意圖轉發
 * <p>
 * 修正：支援 PRESS/RELEASE 動作以及 ROTATE (MOVE) 意圖發送。
 */
@Mixin(MouseHandler.class)
public abstract class MouseInputMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Unique
    private static final int BUTTON_LEFT  = 0;
    @Unique
    private static final int BUTTON_RIGHT = 1;

    /**
     * 攔截滑鼠轉動並轉發為意圖
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void myulib_mc$onMouseTurn(CallbackInfo ci) {
        ClientControlManager manager = ClientControlManager.INSTANCE;
        
        // 1. 如果正在控制，發送旋轉意圖 (包含位移增量)
        if (manager.isControlling()) {
            if (this.accumulatedDX != 0) {
                manager.sendIntent(Intent.of(
                        IntentType.ROTATE_YAW, 
                        InputAction.MOVE, 
                        new Vec3(this.accumulatedDX, 0, 0),
                        0, null
                ));
            }
            if (this.accumulatedDY != 0) {
                manager.sendIntent(Intent.of(
                        IntentType.ROTATE_PITCH, 
                        InputAction.MOVE, 
                        new Vec3(0, this.accumulatedDY, 0),
                        0, null
                ));
            }
            // 兼容舊版綜合意圖
            manager.sendIntent(Intent.of(
                    IntentType.ROTATE, 
                    InputAction.MOVE, 
                    new Vec3(this.accumulatedDX, this.accumulatedDY, 0),
                    0, 
                    null
            ));
        }

        // 2. 虛擬準心劫持
        if (manager.isLockedOn()) {
            manager.updateVirtualCrosshair(this.accumulatedDX, this.accumulatedDY);
            this.accumulatedDX = 0;
            this.accumulatedDY = 0;
            ci.cancel();
            return;
        }

        // 3. 權限攔截
        if (manager.shouldBlockRotation()) {
            ci.cancel();
        }
    }

    /**
     * 攔截滑鼠按鍵並轉發為 PRESS/RELEASE 意圖
     */
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true, require = 0)
    private void myulib_mc$onMouseButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        ClientControlManager manager = ClientControlManager.INSTANCE;

        // 🌟 修正：如果正在開啟介面 (UI)，允許所有滑鼠點擊，不進行攔截。
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen != null) return;

        // 1. 權限攔截 (按下時檢查)
        if (action == 1) { // 1 = PRESS
            if (rawButtonInfo.button() == BUTTON_LEFT && manager.shouldBlockLeftClick()) {
                ci.cancel();
                return;
            }
            if (rawButtonInfo.button() == BUTTON_RIGHT && manager.shouldBlockRightClick()) {
                ci.cancel();
                return;
            }
            if (rawButtonInfo.button() == 2 && manager.isDenied(ControlType.MIDDLE_CLICK)) {
                ci.cancel();
                return;
            }
        }

        // 2. 遙控意圖轉發
        if (manager.isControlling()) {
            InputAction inputAction = switch (action) {
                case 1 -> InputAction.PRESS;
                case 0 -> InputAction.RELEASE;
                case 2 -> InputAction.REPEAT;
                default -> null;
            };

            if (inputAction != null) {
                IntentType type = switch (rawButtonInfo.button()) {
                    case BUTTON_LEFT -> IntentType.LEFT_CLICK;
                    case BUTTON_RIGHT -> IntentType.RIGHT_CLICK;
                    default -> null;
                };

                if (type != null) {
                    manager.sendIntent(Intent.action(type, inputAction));
                } else if (rawButtonInfo.button() == 2) {
                    manager.sendIntent(Intent.action(IntentType.MIDDLE_CLICK, inputAction));
                }
            }
        }
    }
}
