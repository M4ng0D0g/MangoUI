package com.myudog.myulib.mixin.framework;

import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionGate;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerDropPermission {

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true, require = 0)
    private void onDrop(ItemStack itemStack, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        Player player = (Player) (Object) this;
        if (PermissionGate.isDenied(player, PermissionAction.DROP_ITEM, player.position())) {
            cir.setReturnValue(null);
        }
    }
}

