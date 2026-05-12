package com.myudog.myulib.mixin.framework;

import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionGate;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntityPermission {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true, require = 0)
    private void onPickup(Player player, CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (PermissionGate.isDenied(player, PermissionAction.PICKUP_ITEM, itemEntity.position())) {
            ci.cancel();
        }
    }
}

