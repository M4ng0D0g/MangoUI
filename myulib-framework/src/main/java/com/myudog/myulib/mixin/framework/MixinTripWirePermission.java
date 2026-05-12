package com.myudog.myulib.mixin.framework;

import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionGate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TripWireBlock.class)
public abstract class MixinTripWirePermission {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true, require = 0)
    private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean movedByPiston, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (PermissionGate.isDenied(player, PermissionAction.TRIGGER_REDSTONE, pos.getCenter())) {
            ci.cancel();
        }
    }
}

