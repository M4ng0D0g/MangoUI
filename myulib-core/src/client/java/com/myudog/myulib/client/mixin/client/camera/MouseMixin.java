package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.api.core.control.ControlType;
import com.myudog.myulib.client.api.control.ClientControlManager;
import com.myudog.myulib.internal.camera.ClientCameraManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {

    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void myulib$lockMouseTurn(CallbackInfo ci) {
        // 1. 如果伺服器明確下達「禁止轉頭 (ROTATE)」的權限剝奪
        if (ClientControlManager.INSTANCE.isDenied(ControlType.ROTATE)) {
            ci.cancel();
            return;
        }

        // 2. 如果攝影機系統目前有強制注視目標 (LookAt)，則剝奪滑鼠自由轉動權限
        if (ClientCameraManager.INSTANCE.getTrackingTarget() != null 
            && ClientCameraManager.INSTANCE.getTrackingTarget().getStaticLookAt() != null) {
            ci.cancel();
        }
    }
}