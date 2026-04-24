package com.myudog.myulib.mixin;

import com.myudog.myulib.api.debug.DebugTraceManager;
import com.myudog.myulib.api.game.core.GameManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class MixinPlayerInteractionManager {

    @Shadow @Final
    protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    // 🎯 攔截 1: 破壞方塊
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        DebugTraceManager.INSTANCE.begin(player, "destroyBlock");
        DebugTraceManager.INSTANCE.step(player, "pos=" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        PermissionDecision decision = PermissionGate.evaluateDecision(player, PermissionAction.BLOCK_BREAK, pos.getCenter());
        DebugTraceManager.INSTANCE.step(player, "decision=" + decision);

        if (decision == PermissionDecision.DENY) {
            cir.setReturnValue(false); // 取消破壞
            DebugTraceManager.INSTANCE.end(player, "result=DENY");
            return;
        }

        boolean canceled = GameManager.handleBlockBreak(player, pos, this.level);
        if (canceled) {
            cir.setReturnValue(false);
            DebugTraceManager.INSTANCE.end(player, "result=GAME_CONSUMED");
            return;
        }
        DebugTraceManager.INSTANCE.end(player, "result=ALLOW");
    }

    // 🎯 攔截 2: 對方塊點擊右鍵 (放置方塊、開箱子、用水桶)
    // Keep compatibility across mapping/version rename: interactBlock -> useItemOn
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, require = 0)
    private void onInteractBlock(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockState state = level.getBlockState(hitResult.getBlockPos());
        PermissionAction action = classifyBlockAction(state, stack);

        DebugTraceManager.INSTANCE.begin(player, "useItemOn");
        DebugTraceManager.INSTANCE.step(player, "action=" + action);
        DebugTraceManager.INSTANCE.step(player, "blockPos=" + hitResult.getBlockPos().getX() + "," + hitResult.getBlockPos().getY() + "," + hitResult.getBlockPos().getZ());
        PermissionDecision decision = PermissionGate.evaluateDecision(player, action, hitResult.getLocation());
        DebugTraceManager.INSTANCE.step(player, "decision=" + decision);
        if (decision == PermissionDecision.DENY) {
            cir.setReturnValue(InteractionResult.FAIL);
            DebugTraceManager.INSTANCE.end(player, "result=DENY");
            return;
        }

        boolean canceled = GameManager.handleBlockInteract(player, hitResult.getBlockPos(), this.level);
        if (canceled) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            DebugTraceManager.INSTANCE.end(player, "result=GAME_CONSUMED");
            return;
        }
        DebugTraceManager.INSTANCE.end(player, "result=ALLOW");
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void onUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        PermissionAction action = classifyItemAction(stack);
        ItemStack snapshot = stack.copy();
        DebugTraceManager.INSTANCE.begin(player, "useItem");
        DebugTraceManager.INSTANCE.step(player, "action=" + action);
        PermissionDecision decision = PermissionGate.evaluateDecision(player, action, player.position());
        DebugTraceManager.INSTANCE.step(player, "decision=" + decision);
        if (decision == PermissionDecision.DENY) {
            // Keep held item unchanged when any item-use permission is denied.
            player.setItemInHand(hand, snapshot);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            cir.setReturnValue(InteractionResult.FAIL);
            DebugTraceManager.INSTANCE.end(player, "result=DENY");
            return;
        }
        DebugTraceManager.INSTANCE.end(player, "result=ALLOW");
    }

    private static PermissionAction classifyBlockAction(BlockState state, ItemStack stack) {
        if (stack.getItem() instanceof BlockItem) return PermissionAction.BLOCK_PLACE;
        if (stack.getItem() instanceof BucketItem) return PermissionAction.USE_BUCKET;
        if (stack.getItem() instanceof FlintAndSteelItem || stack.getItem() instanceof FireChargeItem) return PermissionAction.IGNITE_BLOCK;
        if (stack.getItem() instanceof SpawnEggItem) return PermissionAction.USE_SPAWN_EGG;

        // Door open/close is regular block interaction, not redstone trigger.
        if (state.getBlock() instanceof DoorBlock) return PermissionAction.INTERACT_BLOCK;
        if (state.hasBlockEntity()) return PermissionAction.OPEN_CONTAINER;
        if (state.hasProperty(BlockStateProperties.POWERED)) return PermissionAction.TRIGGER_REDSTONE;
        if (state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)) {
            return PermissionAction.USE_PORTAL;
        }

        return PermissionAction.INTERACT_BLOCK;
    }

    private static PermissionAction classifyItemAction(ItemStack stack) {
        if (stack.getItem() instanceof SpawnEggItem) return PermissionAction.USE_SPAWN_EGG;
        if (stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof SnowballItem
                || stack.getItem() instanceof EggItem
                || stack.getItem() instanceof EnderpearlItem) {
            return PermissionAction.USE_PROJECTILE;
        }
        return PermissionAction.USE_ITEM;
    }
}
