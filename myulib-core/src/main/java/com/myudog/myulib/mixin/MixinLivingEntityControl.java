package com.myudog.myulib.mixin;

import com.myudog.myulib.api.core.control.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 為所有 LivingEntity 注入控制能力。
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityControl implements 
    IControllableMovable, 
    IControllableAttackable, 
    IControllableRotatable,
    IControllableActionable,
    IControllableInteractable {

    @Unique
    private Vec3 movementIntent = Vec3.ZERO;

    @Unique
    private Vec3 rotationIntent = null;

    // ── Shadow 原版欄位與方法 ──────────────────────────────────────────────
    @Shadow public float xxa;
    @Shadow public float zza;
    @Shadow protected boolean jumping;
    @Shadow public abstract void setYHeadRot(float yHeadRot);
    @Shadow public float yBodyRot;
    @Shadow public abstract void setSneaking(boolean sneaking);
    @Shadow public abstract void setSprinting(boolean sprinting);
    @Shadow public abstract void swing(InteractionHand hand);


    // ── IControllable 基礎實作 ──────────────────────────────────────────────

    @Override
    public UUID myulib_mc$getControllableUuid() {
        return selfUuid();
    }

    // ── IControllableMovable 實作 ───────────────────────────────────────────

    @Override
    public void myulib_mc$executeMove(Vec3 movementVector) {
        this.movementIntent = movementVector;
    }

    // ── IControllableRotatable 實作 ───────────────────────────────────────

    @Override
    public void updateRotation(float yaw, float pitch) {
        this.rotationIntent = new Vec3(yaw, pitch, 0);
    }

    @Override
    public boolean shouldSyncRotation() {
        return true;
    }

    // ── IControllableActionable 實作 ───────────────────────────────────────

    @Override
    public void executeAction(Intent intent) {
        boolean pressed = intent.action() == InputAction.PRESS;
        switch (intent.type()) {
            case JUMP -> this.jumping = pressed;
            case SNEAK -> this.setSneaking(pressed);
            case SPRINT -> this.setSprinting(pressed);
        }
    }

    // ── IControllableInteractable 實作 ─────────────────────────────────────

    @Override
    public void executeInteract(Intent intent) {
        if (intent.action() == InputAction.PRESS) {
            this.swing(InteractionHand.MAIN_HAND);
        }
    }

    // ── IControllableAttackable 實作 ───────────────────────────────────────

    @Override
    public void myulib_mc$executeAttack(Intent intent) {
        if (intent.action() == InputAction.PRESS) {
            this.swing(InteractionHand.MAIN_HAND);
        }
    }


    // ── aiStep 注入：覆寫實體移動與朝向 ──────────────────────────────────────

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void myulib_mc$overrideAiAndMovement(CallbackInfo ci) {
        if (!ControlManager.INSTANCE.isControlledTarget(selfUuid())) return;

        // 1. 視角旋轉控制
        if (rotationIntent != null) {
            float targetYaw = (float) rotationIntent.x;
            float targetPitch = (float) rotationIntent.y;
            
            ((net.minecraft.world.entity.Entity) (Object) this).setYRot(targetYaw);
            ((net.minecraft.world.entity.Entity) (Object) this).setXRot(targetPitch);
            this.setYHeadRot(targetYaw);
            this.yBodyRot = targetYaw;
        } 
        else if (movementIntent.lengthSqr() > 0.0001) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(-movementIntent.x, movementIntent.z));
            ((net.minecraft.world.entity.Entity) (Object) this).setYRot(targetYaw);
            this.setYHeadRot(targetYaw);
            this.yBodyRot = targetYaw;
        }

        // 2. 移動控制 (zza 為前進/後退)
        if (movementIntent.lengthSqr() > 0.0001) {
            this.zza = (float) movementIntent.length();
        } else {
            this.zza = 0;
        }
        this.xxa = 0;
    }


    // ── die() 注入 ─────────────────────────────────────────────────────────

    @Inject(method = "die", at = @At("HEAD"))
    @SuppressWarnings("resource")
    private void myulib_mc$cleanupOnDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ServerLevel level = (ServerLevel) self.level();
        if (level.getServer() != null) {
            ControlManager.INSTANCE.unbindTarget(self.getUUID(), level.getServer());
        }
    }

    @Unique
    private UUID selfUuid() {
        return ((LivingEntity) (Object) this).getUUID();
    }
}