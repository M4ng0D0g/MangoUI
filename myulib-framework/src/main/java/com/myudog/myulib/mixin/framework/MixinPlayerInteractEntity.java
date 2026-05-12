package com.myudog.myulib.mixin.framework;

import com.myudog.myulib.api.core.debug.DebugTraceManager;
import com.myudog.myulib.api.core.object.ObjectManager;
import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionDecision;
import com.myudog.myulib.api.framework.permission.PermissionGate;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerInteractEntity {

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;

        PermissionAction action = PermissionAction.INTERACT_ENTITY;
        if (entity instanceof ArmorStand) action = PermissionAction.ARMOR_STAND_MANIPULATE;
        else if (entity instanceof PlayerRideableJumping || entity instanceof VehicleEntity || entity instanceof HappyGhast) {
            action = PermissionAction.RIDE_ENTITY;
        }

        if (serverPlayer != null) {
            DebugTraceManager.INSTANCE.begin(serverPlayer, "interactEntity");
            DebugTraceManager.INSTANCE.step(serverPlayer, "action=" + action);
            DebugTraceManager.INSTANCE.step(serverPlayer, "target=" + entity.getType());
        }

        PermissionDecision decision = serverPlayer != null
                ? PermissionGate.evaluateDecision(serverPlayer, action, entity.position())
                : (PermissionGate.isDenied(player, action, entity.position()) ? PermissionDecision.DENY : PermissionDecision.ALLOW);

        if (serverPlayer != null) {
            DebugTraceManager.INSTANCE.step(serverPlayer, "decision=" + decision);
        }

        if (decision == PermissionDecision.DENY) {
            cir.setReturnValue(InteractionResult.FAIL);
            if (serverPlayer != null) {
                DebugTraceManager.INSTANCE.end(serverPlayer, "result=DENY");
            }
            return;
        }

        if (serverPlayer == null) {
            return;
        }

        boolean canceled = ObjectManager.INSTANCE.handleEntityInteract(serverPlayer, entity, hand);
        if (canceled) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            DebugTraceManager.INSTANCE.end(serverPlayer, "result=GAME_CONSUMED");
            return;
        }

        DebugTraceManager.INSTANCE.end(serverPlayer, "result=ALLOW");
    }
}