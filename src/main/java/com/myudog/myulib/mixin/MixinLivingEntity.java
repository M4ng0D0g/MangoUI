package com.myudog.myulib.mixin;

import com.myudog.myulib.api.debug.DebugFeature;
import com.myudog.myulib.api.debug.DebugLogManager;
import com.myudog.myulib.api.debug.DebugTraceManager;
import com.myudog.myulib.api.effect.ISpatialEffectManager;
import com.myudog.myulib.api.game.core.GameManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionGate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    // Legacy hook (older mappings/versions)
//    @Inject(method = "actuallyHurt", at = @At("HEAD"), require = 0)
//    private void onHurt(ServerLevel level, DamageSource source, float dmg, CallbackInfo ci) {
//        LivingEntity victim = (LivingEntity) (Object) this;
//        GameManager.handleEntityDamage(victim, source, dmg);
//    }

    // 26.1+ hook
    @Inject(method = "hurtServer", at = @At("HEAD"), require = 0)
    private void onHurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;

        if (source.getEntity() instanceof ServerPlayer attacker) {
            PermissionAction action = classifyAttackAction(victim);
            DebugTraceManager.INSTANCE.begin(attacker, "hurtServer");
            DebugTraceManager.INSTANCE.step(attacker, "action=" + action);
            DebugTraceManager.INSTANCE.step(attacker, "victim=" + victim.getType());
            PermissionDecision decision = PermissionGate.evaluateDecision(attacker, action, victim.position());
            DebugTraceManager.INSTANCE.step(attacker, "decision=" + decision);
            DebugLogManager.INSTANCE.log(DebugFeature.PERMISSION,
                    "hurtServer action=" + action + ",decision=" + decision
                            + ",attacker=" + attacker.getName().getString()
                            + ",victim=" + victim.getType());
            if (decision == PermissionDecision.DENY) {
                cir.setReturnValue(false);
                DebugTraceManager.INSTANCE.end(attacker, "result=DENY");
                return;
            }
            DebugTraceManager.INSTANCE.end(attacker, "result=ALLOW");
        }

        if (victim instanceof ServerPlayer victimPlayer) {
            DebugLogManager.INSTANCE.log(DebugFeature.PERMISSION,
                    "received_damage victim=" + victimPlayer.getName().getString()
                            + ",source=" + source.getMsgId() + ",amount=" + amount);
        }

        GameManager.handleEntityDamage(victim, source, amount);
    }

    @Inject(method = "hurt", at = @At("HEAD"), require = 0)
    private void onHurtCompat(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (source.getEntity() instanceof ServerPlayer attacker) {
            PermissionAction action = classifyAttackAction(victim);
            PermissionDecision decision = PermissionGate.evaluateDecision(attacker, action, victim.position());
            DebugLogManager.INSTANCE.log(DebugFeature.PERMISSION,
                    "hurt action=" + action + ",decision=" + decision
                            + ",attacker=" + attacker.getName().getString()
                            + ",victim=" + victim.getType());
            if (decision == PermissionDecision.DENY) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity victim = (LivingEntity) (Object) this;
        GameManager.handleEntityDeath(victim, damageSource);
    }

    @Inject(method = "removeAllEffects", at = @At("HEAD"), cancellable = true)
    private void onRemoveAllEffects(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        ISpatialEffectManager effectManager = GameManager.getGlobalEffectManager();
        if (!effectManager.hasAnySpatialEffect(player.getUUID())) {
            return;
        }

        List<Holder<MobEffect>> removable = new ArrayList<>();
        for (MobEffectInstance effectInstance : player.getActiveEffects()) {
            Holder<MobEffect> holder = effectInstance.getEffect();
            if (!effectManager.isSpatialEffect(player.getUUID(), holder)) {
                removable.add(holder);
            }
        }

        for (Holder<MobEffect> holder : removable) {
            player.removeEffect(holder);
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
    private void onRemoveEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        ISpatialEffectManager effectManager = GameManager.getGlobalEffectManager();
        if (effectManager.isSpatialEffect(player.getUUID(), effect)) {
            cir.setReturnValue(false);
        }
    }

    private static PermissionAction classifyAttackAction(LivingEntity victim) {
        if (victim instanceof Player) {
            return PermissionAction.ATTACK_PLAYER;
        }
        if (victim instanceof Animal || victim instanceof Villager) {
            return PermissionAction.ATTACK_FRIENDLY_MOB;
        }
        return PermissionAction.ATTACK_HOSTILE_MOB;
    }
}
