package com.myudog.myulib.client.mixin.client;

import com.myudog.myulib.client.api.control.ClientControlManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouse {

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void blockRotationWhenDisabled(CallbackInfo ci) {
        if (ClientControlManager.INSTANCE.shouldBlockRotation()) {
            ci.cancel();
        }
    }
}

