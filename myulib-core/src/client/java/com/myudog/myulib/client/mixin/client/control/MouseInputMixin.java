package com.myudog.myulib.client.mixin.client.control;

import com.myudog.myulib.api.core.control.InputAction;
import com.myudog.myulib.api.core.control.Intent;
import com.myudog.myulib.api.core.control.IntentType;
import com.myudog.myulib.client.api.control.ClientControlManager;
import net.minecraft.client.MouseHandler;
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

    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

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
            manager.sendIntent(Intent.of(
                    IntentType.ROTATE, 
                    InputAction.MOVE, 
                    new Vec3(this.cursorDeltaX, this.cursorDeltaY, 0), 
                    0, 
                    null
            ));
        }

        // 2. 虛擬準心劫持
        if (manager.isLockedOn()) {
            manager.updateVirtualCrosshair(this.cursorDeltaX, this.cursorDeltaY);
            this.cursorDeltaX = 0;
            this.cursorDeltaY = 0;
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
    private void myulib_mc$onMouseButton(long handle, int button, int action, int mods, CallbackInfo ci) {
        ClientControlManager manager = ClientControlManager.INSTANCE;

        // 1. 權限攔截 (按下時檢查)
        if (action == 1) { // 1 = PRESS
            if (button == BUTTON_LEFT && manager.shouldBlockLeftClick()) {
                ci.cancel();
                return;
            }
            if (button == BUTTON_RIGHT && manager.shouldBlockRightClick()) {
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
                IntentType type = switch (button) {
                    case BUTTON_LEFT -> IntentType.LEFT_CLICK;
                    case BUTTON_RIGHT -> IntentType.RIGHT_CLICK;
                    default -> null;
                };

                if (type != null) {
                    manager.sendIntent(Intent.action(type, inputAction));
                }
            }
        }
    }
}
