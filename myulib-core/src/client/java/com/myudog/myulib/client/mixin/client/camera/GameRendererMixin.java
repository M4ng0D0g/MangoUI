package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.client.api.camera.ClientCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void myulib$overrideFov(CallbackInfoReturnable<Double> cir) {
        double originalFov = cir.getReturnValue();
        float modifiedFov = ClientCameraManager.INSTANCE.getModifiedFov((float) originalFov);
        
        // 如果 modifiedFov 與 originalFov 不同，則覆寫傳回值
        if (Math.abs(modifiedFov - originalFov) > 0.001f) {
            cir.setReturnValue((double) modifiedFov);
        }
    }
}
