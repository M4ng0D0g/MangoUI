package com.myudog.myulib.mixin;

import com.myudog.myulib.api.control.ControlManager;
import com.myudog.myulib.api.control.ControlType;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionGate;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerPlayerControlPermission {

    @Shadow @Final public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true, require = 0)
    private void onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (PermissionGate.isDenied(player, PermissionAction.INVENTORY_MOVE, player.position())) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true, require = 0)
    private void onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
                && PermissionGate.isDenied(player, PermissionAction.INVENTORY_MOVE, player.position())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        boolean denyMove = packet.hasPosition()
                && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.MOVE);
        boolean denyLook = packet.hasRotation()
                && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.ROTATE);

        if (denyMove || denyLook) {
            ci.cancel();

            double x = denyMove ? player.getX() : packet.getX(player.getX());
            double y = denyMove ? player.getY() : packet.getY(player.getY());
            double z = denyMove ? player.getZ() : packet.getZ(player.getZ());
            float yaw = denyLook ? player.getYRot() : packet.getYRot(player.getYRot());
            float pitch = denyLook ? player.getXRot() : packet.getXRot(player.getXRot());

            // Keep allowed axis (movement or rotation) and reject only denied axis.
            player.connection.teleport(x, y, z, yaw, pitch);
        }
    }

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void onPlayerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        ServerboundPlayerCommandPacket.Action action = packet.getAction();

        if (action == ServerboundPlayerCommandPacket.Action.START_SPRINTING
                && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.SPRINT)) {
            ci.cancel();
            player.setSprinting(false);
            return;
        }

        String actionName = action.name();
        boolean sneakStart = actionName.contains("SHIFT") || actionName.contains("SNEAK");
        if (sneakStart) {
            boolean denySneak = !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.SNEAK);
            boolean denyCrawl = player.isSwimming() && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.CRAWL);
            if (denySneak || denyCrawl) {
                ci.cancel();
                player.setShiftKeyDown(false);
            }
        }
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true, require = 0)
    private void onPlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        var input = packet.input();
        boolean blocked = false;

        if (input.sprint() && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.SPRINT)) {
            player.setSprinting(false);
            blocked = true;
        }

        if (input.shift() && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.SNEAK)) {
            player.setShiftKeyDown(false);
            blocked = true;
        }

        if (input.jump() && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.JUMP)) {
            blocked = true;
        }

        if (player.isSwimming() && !ControlManager.INSTANCE.isPlayerControlEnabled(player, ControlType.CRAWL)) {
            player.setShiftKeyDown(false);
            blocked = true;
        }

        if (blocked) {
            ci.cancel();
        }
    }
}

