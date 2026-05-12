package com.myudog.myulib.mixin.framework;

import com.myudog.myulib.api.core.debug.DebugFeature;
import com.myudog.myulib.api.core.debug.DebugLogManager;
import com.myudog.myulib.api.core.debug.DebugTraceManager;
import com.myudog.myulib.api.framework.permission.PermissionAction;
import com.myudog.myulib.api.framework.permission.PermissionDecision;
import com.myudog.myulib.api.framework.permission.PermissionGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayerEntity {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(Entity target, CallbackInfo ci) {
        Player attacker = (Player) (Object) this;
        if (!(attacker instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PermissionAction action = classifyAttackAction(attacker, target);

        DebugTraceManager.INSTANCE.begin(serverPlayer, "attack");
        DebugTraceManager.INSTANCE.step(serverPlayer, "action=" + action);
        DebugTraceManager.INSTANCE.step(serverPlayer, "target=" + target.getType().toString());
        PermissionDecision decision = PermissionGate.evaluateDecision(serverPlayer, action, target.position());
        DebugTraceManager.INSTANCE.step(serverPlayer, "decision=" + decision);
        DebugLogManager.INSTANCE.log(DebugFeature.PERMISSION,
                "attack action=" + action + ",decision=" + decision
                        + ",attacker=" + attacker.getName().getString()
                        + ",target=" + target.getType());

        if (decision == PermissionDecision.DENY) {
            ci.cancel();
            DebugTraceManager.INSTANCE.end(serverPlayer, "result=DENY");
            return;
        }

        DebugTraceManager.INSTANCE.end(serverPlayer, "result=ALLOW");
    }

    private static PermissionAction classifyAttackAction(Player attacker, Entity target) {
        // Spear/trident left-click dash should follow USE_ITEM policy instead of attack policy.
        if (attacker.getMainHandItem().getItem() instanceof TridentItem) {
            return PermissionAction.USE_ITEM;
        }
        if (target instanceof Player) {
            return PermissionAction.ATTACK_PLAYER;
        }
        if (target instanceof Animal || target instanceof Villager) {
            return PermissionAction.ATTACK_FRIENDLY_MOB;
        }
        return PermissionAction.ATTACK_HOSTILE_MOB;
    }
}
