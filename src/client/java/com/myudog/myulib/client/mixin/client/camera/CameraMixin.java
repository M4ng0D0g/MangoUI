package com.myudog.myulib.client.mixin.client.camera;

import com.myudog.myulib.client.api.camera.ClientCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void myulib$applyCameraModifiers(BlockGetter level, Entity focusedEntity, boolean detached, boolean mirrored, float tickDelta, CallbackInfo ci) {
        ClientCameraManager.getInstance().applyAll((Camera) (Object) this, tickDelta);
    }
}


