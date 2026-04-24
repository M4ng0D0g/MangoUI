package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.client.api.camera.ClientCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void myulib$applyCameraModifiers(DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientCameraManager.INSTANCE.getInstance().applyAll((Camera) (Object) this, 1.0f);
    }
}


