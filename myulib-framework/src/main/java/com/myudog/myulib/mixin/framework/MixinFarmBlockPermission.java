package com.myudog.myulib.mixin.framework;

import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionGate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class MixinFarmBlockPermission {

    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true, require = 0)
    private void onTrample(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (!state.is(Blocks.FARMLAND)) {
            return;
        }
        if (entity instanceof Player player && PermissionGate.isDenied(player, PermissionAction.TRAMPLE_FARMLAND, pos.getCenter())) {
            ci.cancel();
        }
    }
}
