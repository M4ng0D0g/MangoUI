package com.myudog.myulib.mixin.framework;

import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerMessagePermission {

    @Shadow @Final public ServerPlayer player;

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMessage(CallbackInfo ci) {
        if (PermissionGate.isDenied(player, PermissionAction.SEND_MESSAGE, player.position())) {
            ci.cancel();
        }
    }
}

