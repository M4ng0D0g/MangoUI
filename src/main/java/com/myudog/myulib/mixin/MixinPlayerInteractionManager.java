package com.myudog.myulib.mixin;

import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerPlayerGameMode.class)
public class MixinPlayerInteractionManager {

    @Shadow @Final
    protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    // 🎯 攔截 1: 破壞方塊
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // 核心：使用被破壞的方塊座標 (pos) 去找 Field，而不是玩家座標！
        Identifier dimId = level.dimension().identifier();
        Optional<FieldDefinition> targetField = FieldManager.findAt(dimId, pos.getCenter());

        PermissionDecision decision = PermissionManager.evaluate(
                player.getUUID(),
                RoleGroupManager.getSortedGroupIdsOf(player.getUUID()),
                PermissionAction.BLOCK_BREAK,
                targetField.map(FieldDefinition::id).orElse(null),
                dimId
        );

        if (decision == PermissionDecision.DENY) {
            cir.setReturnValue(false); // 取消破壞
        }
    }

    // 🎯 攔截 2: 對方塊點擊右鍵 (放置方塊、開箱子、用水桶)
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos targetPos = hitResult.getBlockPos();
        Identifier dimId = level.dimension().identifier();
        Optional<FieldDefinition> targetField = FieldManager.findAt(dimId, hitResult.getLocation());

        // 判斷動作：手拿方塊就是 PLACE，拿水桶就是 USE_BUCKET，空手就是 INTERACT
        PermissionAction action = PermissionAction.INTERACT_BLOCK;
        if (stack.getItem() instanceof BlockItem) action = PermissionAction.BLOCK_PLACE;
        else if (stack.getItem() instanceof BucketItem) action = PermissionAction.USE_BUCKET;

        PermissionDecision decision = PermissionManager.evaluate(
                player.getUUID(),
                RoleGroupManager.getSortedGroupIdsOf(player.getUUID()),
                action,
                targetField.map(FieldDefinition::id).orElse(null),
                dimId
        );

        if (decision == PermissionDecision.DENY) {
            cir.setReturnValue(InteractionResult.FAIL); // 阻止右鍵行為
        }
    }
}
