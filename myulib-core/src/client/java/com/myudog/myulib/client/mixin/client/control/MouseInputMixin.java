package com.myudog.myulib.client.mixin.client.control;

import com.myudog.myulib.client.api.control.ClientControlManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在滑鼠按鍵的物理輸入層攔截左鍵 / 右鍵。
 * <p>
 * {@code onPress} 是 GLFW 的 mouse button callback，
 * 在 Minecraft 解析封包前最早觸發，適合作為輸入閘門的硬性攔截點。
 * <p>
 * 攔截順序（由低至高）：
 * <ol>
 *   <li>GLFW 物理回呼 → 此 Mixin 攔截（最早）</li>
 *   <li>KeyboardInput#tick → applyClientInputGuards（清除 key binding state）</li>
 *   <li>GameRenderer → Server 封包發送（最晚）</li>
 * </ol>
 */
@Mixin(MouseHandler.class)
public abstract class MouseInputMixin {

    @org.spongepowered.asm.mixin.Shadow private double cursorDeltaX;
    @org.spongepowered.asm.mixin.Shadow private double cursorDeltaY;

    // GLFW button codes: 0 = 左鍵, 1 = 右鍵, 2 = 中鍵
    @Unique
    private static final int BUTTON_LEFT  = 0;
    @Unique
    private static final int BUTTON_RIGHT = 1;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void myulib_mc$hijackMouseForCrosshair(CallbackInfo ci) {
        if (ClientControlManager.INSTANCE.isLockedOn()) {
            // 讀取 dx, dy 更新第二準心座標
            ClientControlManager.INSTANCE.updateVirtualCrosshair(this.cursorDeltaX, this.cursorDeltaY);

            // 清除原生 dx/dy 避免視角轉動
            this.cursorDeltaX = 0;
            this.cursorDeltaY = 0;
            ci.cancel();
            return;
        }

        if (ClientControlManager.INSTANCE.shouldBlockRotation()) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true, require = 0)
    private void myulib_mc$blockMouseButtons(long handle, int button, int action, int mods, CallbackInfo ci) {
        if (action == 0) return;

        if (button == BUTTON_LEFT && ClientControlManager.INSTANCE.shouldBlockLeftClick()) {
            ci.cancel();
            return;
        }
        if (button == BUTTON_RIGHT && ClientControlManager.INSTANCE.shouldBlockRightClick()) {
            ci.cancel();
        }
    }
}
